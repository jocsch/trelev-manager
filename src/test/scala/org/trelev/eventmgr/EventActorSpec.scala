package org.trelev.eventmgr

import org.specs2.mutable.SpecificationLike
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.util.Timeout
import org.specs2.time.NoTimeConversions
import org.trelev.eventmgr.domain._
import org.trelev.eventmgr.domain.EventActor._
import scala.util.Success
import scala.util.Try
import org.trelev.eventmgr.domain.EventActor

//class RegistrationActorSpec extends TestKit(ActorSystem("testsystem")) with SpecificationLike with CoreActors with Core with ImplicitSender {
class EventActorSpec extends TestKit(ActorSystem("testsystem")) with SpecificationLike with NoTimeConversions with CoreActors with Core with ImplicitSender {
  import EventActor._

  sequential
  
  override val event = TestActorRef[EventActor]
  val ua = event.underlyingActor;
  
  
  "Event registration should" >> {

    "add valid events" in {
      /*event ! CreateEvent(Event(None, TenantID("test"), "title", "desc", Host("first","last","hmail"), Location("city","place"), null, null, 3))
      val id = expectMsg(EventID(_))
      print(s"ID: $id")
      success*/
      ua.eventCreated(EventCreated(Event(EventID("haha"), TenantID("test"), "title", "desc", Host("first","last","hmail", "7777"), Location("city","place"), null, null, 3)))
      ua.events must haveKey(EventID("haha"))
      ua.events.size === 1
      
      ua.eventCreated(EventCreated(Event(EventID("xxx"), TenantID("test"), "title", "desc", Host("first","last","hmail", "7777"), Location("city","place"), null, null, 3)))
      ua.events.size === 2
      ua.events must haveKey(EventID("haha"))
      ua.events must haveKey(EventID("xxx"))
      //event ! GetState[Event](EventID)
      //registration ! Register(mkUser(""))
      /*expectMsgPF() {
        //case t@Tuple2[Map[EventID, Event], Map[EventID, Seq[Participant]]] => print(t._1); success
        case "ha" => success
      }*/
      //success
    }
    
    "add participant" in {
      ua.events must have size(2)
      ua.participantRegistered(ParticipantRegistered(EventID("haha"), Participant("first", "last", "email1", Entourage(1,2))))
      ua.events must have size(2)
      ua.participants.keys must have size(1)
      ua.participants must haveKey(EventID("haha"))
      ua.participants(EventID("haha")) must have size(1)
      ua.participants(EventID("haha")) must contain((x: Participant) => x.email === "email1")
      
      ua.participantRegistered(ParticipantRegistered(EventID("haha"), Participant("first", "last", "email2", Entourage(1,2))))
      ua.participants.keys must have size(1)
      ua.participants must haveKey(EventID("haha"))
      ua.participants(EventID("haha")) must have size(2)
      ua.participants(EventID("haha")) must contain((x: Participant) => x.email === "email1")
      ua.participants(EventID("haha")) must contain((x: Participant) => x.email === "email2")
      
      ua.participantRegistered(ParticipantRegistered(EventID("xxx"), Participant("first", "last", "email3", Entourage(1,2))))
      ua.participants.keys must have size(2)
      ua.participants must haveKey(EventID("haha"))
      ua.participants must haveKey(EventID("xxx"))
      ua.participants(EventID("haha")) must have size(2)
      ua.participants(EventID("haha")) must contain((x: Participant) => x.email === "email1")
      ua.participants(EventID("haha")) must contain((x: Participant) => x.email === "email2")
      
      ua.participants(EventID("xxx")) must have size(1)
      ua.participants(EventID("xxx")) must contain((x: Participant) => x.email === "email3")
      success
    }
    
    "remove participant" in {
    	ua.participantUnregistered(ParticipantUnregistered(EventID("haha"), "email1"))
	    ua.participants.keys must have size(2)
	    ua.participants must haveKey(EventID("haha"))
	    ua.participants must haveKey(EventID("xxx"))
	    ua.participants(EventID("haha")) must have size(1)
	    ua.participants(EventID("haha")) must contain((x: Participant) => x.email === "email2")
	      
	    ua.participants(EventID("xxx")) must have size(1)
	    ua.participants(EventID("xxx")) must contain((x: Participant) => x.email === "email3")
    }
    
    "allow cancelation" in {
      ua.events.size === 2
      ua.eventCanceled(EventCanceled(EventID("nixda")))
      ua.events.size === 2
      ua.eventCanceled(EventCanceled(EventID("haha")))
      ua.events.size === 1
      ua.events must haveKey(EventID("xxx"))
      
      ua.participants.keys must have size(1)
	  ua.participants(EventID("xxx")) must have size(1)
	  ua.participants(EventID("xxx")) must contain((x: Participant) => x.email === "email3")
    }
    
    "Direct Actor communication" in {
      import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Await
      implicit val timeout = Timeout(1 second)
      
      val future = event.ask(GetEventState(EventID("xxx"))).mapTo[Option[Event]]
      val Success(result) = future.value.get
      result.get.id.value  must be("xxx")
      //val future = event (? GetEventState(EventID("xxx"))(5 seconds))
    }

    /*"accept valid user to be registered" in {
      registration ! Register(mkUser("jan@eigengo.com"))
      expectMsg(Right(Registered))
      success
    }*/
  }

}
