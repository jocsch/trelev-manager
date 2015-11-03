package org.trelev.eventmgr.mail

import akka.actor.Actor
import courier._
import courier.Defaults._
import javax.mail.internet.InternetAddress
import akka.actor.ActorLogging
import com.typesafe.config.Config
import java.util.regex.Pattern
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.trelev.eventmgr.domain.Participant
import org.trelev.eventmgr.domain.Event
import scala.concurrent.Future
import akka.actor.Props
import akka.actor.OneForOneStrategy
import org.trelev.eventmgr.mail.MailSender.SendingFailedException
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy.Restart
import org.trelev.eventmgr.mail.MailSender.MailData


case class BroadMail(event: Event, participant: Participant, template: String)
case class ParticipantMail(event: Event, participant: Participant, template: String)

class EventMailer extends Actor with ActorLogging {
  import context.become
  
  //this is important as the courier mail sending future might block on smtp problems.
  // a separate threadpool prevents thread starvation for the webserving requests.
  implicit val executionContext = context.system.dispatchers.lookup("mail-dispatcher")
  
  //restart the mailer actor 5 times over 20 minutes to redeliver the message
  override val supervisorStrategy = OneForOneStrategy(5, 20 minutes) {
    case _ => Restart
  }
  
  val config = context.system.settings.config
  //TODO process list
  val tenant = config.getConfig("trelev").getConfigList("tenants").get(0)
  val mailConfig = tenant.getConfig("email-server-config")
  
  val disabled = mailConfig.getBoolean("disable-sending")
  
  val mailerBuilder = Mailer(mailConfig.getString("smtp-server"), mailConfig.getInt("smtp-port"))
               .auth(true)
               .as(mailConfig.getString("user"), mailConfig.getString("pw"))
   
                              
   var mailer:Mailer = null;
  
   if (!disabled) {
	   mailer = if (mailConfig.getBoolean("tls")) {
	      mailerBuilder.startTtls(true)()
	   } else mailerBuilder()
     log.info("E-mail sender: Using " + mailConfig.getString("smtp-server"))
   } else {
     log.info("E-mail sender: Dummy e-mail sender enabled!")
     become(dummy)
   }
  
   def receive = {
     case BroadMail(event, participant, template)  => sendToAll(event, participant, template)
     case ParticipantMail(event, participant, template)  => {
        sendMail(processTemplate(tenant.getString(s"$template-title"), assembleMailParams(event, participant)),
                 processTemplate(tenant.getString(s"$template-body"), assembleMailParams(event, participant)),
                 participant.email)
     }
       
   }
  
  def dummy: Receive = {
    case req@_ => { log.info("Disabled mail sending. Received request: " + req); 
							      /* This comes in handy to test the separate execution context for smtp connections
                     for (i <- 0 to 20) {
	                    Future {println("---------------Sleep start"); Thread.sleep(3000); println("-----------Sleep end");}
                     } 
                    */
							    }
  }
  
  def sendToAll(event: Event, participant: Participant, template: String) = {
		    import scala.collection.JavaConverters._
        
        val params = assembleMailParams(event, participant)
        sendMail(processTemplate(tenant.getString(s"$template-title"), params),
                 processTemplate(tenant.getString(s"$template-body"), params),
                 participant.email)
        
        if (!event.host.email.isEmpty()) {
	        sendMail(processTemplate(tenant.getString(s"$template-notify-title"), params),
	                 processTemplate(tenant.getString(s"$template-notify-body"), params),
	                 event.host.email)
        }
        
        tenant.getStringList("bcc").asScala.foreach { bcc => 
	        sendMail(processTemplate(tenant.getString(s"$template-bcc-title"), params),
	                 processTemplate(tenant.getString(s"$template-bcc-body"), params),
	                 bcc)
        }
  }
  
  
  
  def sendMail(title: String, body: String, recipient: String) {
         val mailSender = context.actorOf(Props(classOf[MailSender], mailer))
         mailSender ! MailData(tenant.getString("sender"), title, body, recipient)
  }
  
  def assembleMailParams(event: Event, par: Participant): Map[String, String] =  {
       val dateFmt = DateTimeFormat.forPattern("dd.MM").withZone(DateTimeZone.forID("Europe/Berlin"));
       val timeFmt = DateTimeFormat.forPattern("H:mm").withZone(DateTimeZone.forID("Europe/Berlin"));
         Map(
            "title" -> event.title,
            "description" -> event.description,
            "date" -> dateFmt.print(event.date),
            "hour" -> timeFmt.print(event.date),
            "duration" -> event.duration.getStandardMinutes.toString(),
            "city" -> event.location.city,
            "location" -> event.location.place,
            "name" -> (par.firstName + " " + par.lastName),
            "email" -> par.email,
            "host" -> (event.host.firstName + " " + event.host.lastName),
            "hostemail" -> event.host.email,
            "hostphone" -> event.host.phone,
            "adults" -> par.entourage.adults.toString(),
            "children" -> par.entourage.children.toString()
          ) 
  }
  
  def processTemplate(template: String, values: Map[String, String]): String = 
     values.foldLeft(template) { case (tmp, (key, value)) =>  tmp.replaceAll(Pattern.quote("{") + key + Pattern.quote("}"), value) }
}