package org.trelev.eventmgr.web

import spray.http.StatusCodes._
import spray.http._
import spray.routing._
//import directives.{CompletionMagnet, RouteDirectives}
import spray.util.{SprayActorLogging, LoggingContext}
import util.control.NonFatal
import spray.httpx.marshalling.Marshaller
import spray.http.HttpHeaders.RawHeader
import akka.actor.{ActorLogging, Actor}

/**
 * Holds potential error response with the HTTP status and optional body
 *
 * @param responseStatus the status code
 * @param response the optional body
 */
case class ErrorResponseException(responseStatus: StatusCode, response: Option[HttpEntity]) extends Exception

/**
 * Allows you to construct Spray ``HttpService`` from a concatenation of routes; and wires in the error handler.
 * It also logs all internal server errors using ``SprayActorLogging``.
 *
 * @param route the (concatenated) route
 */
class RoutedHttpService(route: Route) extends Actor with HttpService with ActorLogging {

  implicit def actorRefFactory = context

  implicit val handler = ExceptionHandler {
    case NonFatal(ErrorResponseException(statusCode, entity)) => ctx =>
      log.info("shit")
      ctx.complete(statusCode, entity)

    case NonFatal(e) => ctx => {
      log.info("shit2")
      log.error("catched")
      log.error(e, InternalServerError.defaultMessage)
      ctx.complete(InternalServerError)
    }
  }


  def receive: Receive =
    runRoute(route)(handler, RejectionHandler.Default, context, RoutingSettings.default, LoggingContext.fromActorRefFactory)


}
