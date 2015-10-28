package org.trelev.eventmgr.domain

import java.util.UUID

import org.trelev.eventmgr.mail._

import com.github.nscala_time.time.Imports._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.event.LoggingReceive
import akka.persistence.PersistentActor

trait FailedCommand{val reason: String; val code: Int}
object Ok

case class SerTest(jup: String)

object EventActor {
  case class CreateEvent(event: Event)
  case class UpdateEvent(event: Event, informParticipants: Boolean)
  case class ListEvents(tenantID: TenantID, pastEvents: Boolean = false)
  case class ShowEvent(id: EventID)
  case class CancelEvent(id: EventID, informParticipants: Boolean)
  case class RegisterParticipant(id: EventID, participant: Participant)
  case class UnregisterParticipant(id: EventID, email: String)
  case class ShowParticipants(id: EventID)
  
  case class RegisterParticipantFailed(reason: String, code: Int) extends FailedCommand
  case class UnregisterParticipantFailed(reason: String, code: Int) extends FailedCommand 
  

  case class EventCreated(event: Event)
  case class EventCanceled(id: EventID)
  case class EventUpdated(event: Event)
  case class ParticipantRegistered(id: EventID, participant: Participant)
  case class ParticipantUnregistered(id: EventID, email: String)
  
  case class GetEventState[T](ev: EventID)
  case class GetParticipientState[T](ev: EventID)
}

class EventActor(mailActor: ActorRef) extends PersistentActor with ActorLogging {
   import EventActor._
   
   override def persistenceId = "events-1"
   
   var events: Map[EventID, Event] = Map()
   var participants: Map[EventID, Seq[Participant]] = Map()
     
   def receiveCommand: Actor.Receive = LoggingReceive {
      case "print" => println(s"received ${events}"); 
      case "boom"  => throw new Exception("boom")
      case ListEvents(tenant, past) => sender ! events.values.filter(_.tenant  == tenant).toSeq.sortWith(_.date < _.date).map(publicEvent(_))
      case ShowEvent(id) => sender ! events.get(id)
      case ShowParticipants(id) => sender ! participants.get(id)
      case GetEventState(id) => sender ! events.get(id)
      case GetParticipientState(id) => sender ! participants.get(id).map(s => s.map(_.email))
      case CreateEvent(ev) => persist(createEvent(ev)) { create =>
        eventCreated(create)
        
        log.info("Event created: " + create.event)
        sender ! create.event
      }
      case CancelEvent(id, inform) => 
         if (!events.contains(id)) {
        	sender ! None
         } else {
          persist(EventCanceled(id)) { cancel => 
            if (inform) {
              val event = events(id)
              participants.get(id).foreach{ parts =>
                parts.foreach{ part =>
				          mailActor ! ParticipantMail(event, part, "cancel")
                }
              }
            }
            
          val ev = events.get(id)
          
          eventCanceled(cancel)
	        log.info(s"Event cancelled: $ev")
	        sender ! Some(Ok) 
          }
      } 
      case UpdateEvent(event, inform) => {
        if (!events.contains(event.id)) {
        	sender ! None
        } else {
	        //what happens if no of participants is now smaller then already registered?
	        persist(EventUpdated(event)) { upd =>
	          eventUpdated(upd)
	          if (inform) {
	            //send e-mail to all participants to inform about update. 
              participants.get(event.id).foreach{ parts =>
                parts.foreach{ part =>
				          mailActor ! ParticipantMail(event, part, "update")
                }
              }
	          }
            log.info(s"Event updated: $event")
	          sender ! Some(upd.event)
	        }
        }
      }
      case RegisterParticipant(id, part) => {
        
        if (!events.contains(id)) {
        	sender ! Left(RegisterParticipantFailed("Event does not exist", 404))
        } else if (pastEvent(events(id))) {
        	sender ! Left(RegisterParticipantFailed("Event is in the past", 501))
        } else if (participants(id).exists(_.email == part.email)) {
        	sender ! Left(RegisterParticipantFailed("E-mailadress already registered", 502))
        } else if (events(id).maxParticipants < numberOfParticipants(participants(id)) + part.entourage.adults + part.entourage.children) {
          sender ! Left(RegisterParticipantFailed("No capacity in event left", 503))
        } else persist(ParticipantRegistered(id, part)) { reg =>
          participantRegistered(reg)
          val event = events(id)
          
          log.info(s"Participant registered: $part  at $event")
          sender ! Right(publicEvent(event))
          
          mailActor ! BroadMail(event, reg.participant, "register")
        }
      }
      case UnregisterParticipant(id, email) => {
    	 participants.get(id).flatMap(_.find(_.email  == email))
    	                     .map{ part =>
    	    if (pastEvent(events(id))) {
    	      sender ! Left(UnregisterParticipantFailed("Can't unregister from past event", 500))
    	    } else persist(ParticipantUnregistered(id, email)) { unreg =>
          val event = events(id)
          mailActor ! BroadMail(event, part, "leave")
          
          log.info(s"Participant unregistered: $part  at $event")
    	 	  participantUnregistered(unreg)
    	 	  sender ! Right(Ok)
    	 	}
    	 }.getOrElse(sender ! Left(UnregisterParticipantFailed("No participant with this e-mail", 404)))
    	   
      
      case _ => 

    }
   
   def pastEvent(ev: Event) = ev.date < DateTime.now
   
   def numberOfParticipants(parts: Seq[Participant]) = numberOfAdults(parts) + numberOfChildren(parts)
   
   def numberOfAdults(parts: Seq[Participant]) = parts.foldLeft(0)((acc, p) => acc + p.entourage.adults)
   def numberOfChildren(parts: Seq[Participant]) = parts.foldLeft(0)((acc, p) => acc + p.entourage.children)
   
   
   def publicEvent(ev: Event) = (ev, participants.get(ev.id).map(numberOfParticipants(_)).getOrElse(0))
   
    def receiveRecover: Actor.Receive = LoggingReceive {
      case e: EventCreated => eventCreated(e)
      case e: EventCanceled => eventCanceled(e)
      case e: EventUpdated => eventUpdated(e)
      case e: ParticipantRegistered => participantRegistered(e)
      case e: ParticipantUnregistered => participantUnregistered(e)
      //case s: String => received = s :: received
    }
    
    def eventCreated(ev: EventCreated) {
    	events = events + ((ev.event.id, ev.event))
    	participants = participants + (ev.event.id -> Nil)
    }
    
    def eventCanceled(ev: EventCanceled) {
        log.info("remove ev: " + ev.id )
    	events = events - ev.id 
    	participants = participants - ev.id
    }
    
    def eventUpdated(ev: EventUpdated) {
    	events = events + ((ev.event.id, ev.event))
    }
    
    def createEvent(ev: Event) =  EventCreated(ev.copy(id = EventID(UUID.randomUUID().toString())))
    
    def participantRegistered(ev: ParticipantRegistered) {
        participants = participants + (ev.id -> (participants.getOrElse(ev.id, Seq()) :+ ev.participant))
    }
    
    def participantUnregistered(ev: ParticipantUnregistered) {
    	for {
    		parts <- participants.get(ev.id)
    		fparts = parts.filterNot(_.email == ev.email)
    	} {
    	  participants = participants + (ev.id -> fparts)
    	}
      
    }
    

}