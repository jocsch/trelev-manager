package org.trelev.eventmgr

import org.trelev.eventmgr.domain.EventActor
import org.trelev.eventmgr.mail.EventMailer

import akka.actor.ActorSystem
import akka.actor.Props

/**
 * Core is type containing the ``system: ActorSystem`` member. This enables us to use it in our
 * apps as well as in our tests.
 */
trait Core {

  implicit def system: ActorSystem

}

/**
 * This trait implements ``Core`` by starting the required ``ActorSystem`` and registering the
 * termination handler to stop the system when the JVM exits.
 */
trait BootedCore extends Core {

  /**
   * Construct the ActorSystem we will use in our application
   */
  implicit lazy val system = ActorSystem("event-manager")

  /**
   * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
   */
  sys.addShutdownHook(system.shutdown())

}

/**
 * This trait contains the actors that make up our application; it can be mixed in with
 * ``BootedCore`` for running code or ``TestKit`` for unit and integration tests.
 */
trait CoreActors {
  this: Core =>

  //val registration = system.actorOf(Props[RegistrationActor])
  val mail = system.actorOf(Props[EventMailer], "MailActor")
  val event = system.actorOf(Props(new EventActor(mail)), "EventActor")
  //val messenger    = system.actorOf(Props[MessengerActor])

}