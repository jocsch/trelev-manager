package org.trelev.eventmgr.mail

import akka.actor.Actor
import courier.Mailer
import courier.Envelope
import javax.mail.internet.InternetAddress
import courier.Text
import akka.actor.ActorLogging

object MailSender {
  case class MailData(sender: String, title: String, body: String, recipient: String)
  class SendingFailedException(msg: String, val mailData: MailData) extends Exception(msg) 
}

class MailSender(mailer: Mailer) extends Actor with ActorLogging {
    import MailSender._
    implicit val executionContext = context.system.dispatchers.lookup("mail-dispatcher")
    
    override def preRestart(reason: Throwable, message:Option[Any]) = {
      log.info("Restarted actor to send again: {}", message)
      //during restart reprocess the failed message. only use the maildata
      message.foreach { case ex:SendingFailedException => self ! ex.mailData}
    }
    
    def receive = {
      case mailData: MailData => sendMail(mailData)
      //rethrow message as exception to let the actor restart
      case ex:SendingFailedException => throw ex 
    }
    
     //def sendMail(sender: String, title: String, body: String, recipient: String) {
     def sendMail(mailData: MailData) = {
       val MailData(sender, title, body, recipient) = mailData
       log.info(s"Sender is <$sender>, Recipient is <$recipient>")
        val sending = mailer(Envelope.from(new InternetAddress(sender))
          .to(new InternetAddress(recipient))
          .subject(title)
          .replyTo(new InternetAddress(sender))
          .content(Text(body)))
        
       sending.onSuccess{case res => log.info(s"E-mail sent to $recipient with title $title. Result: " + res); context.stop(self);}
       sending.onFailure{ case ex => 
         log.warning(s"E-mail sent to $recipient with title $title failed: " + ex)
         //because we are in a future (different thread/context) we can not simply throw the exception
         // but instead we have to send it to self. 
         self ! new SendingFailedException(ex.getMessage, mailData)   
       }
    }
}