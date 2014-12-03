package org.trelev.eventmgr.web

import scala.concurrent.ExecutionContext
import org.joda.time.DateTime
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.util.Timeout
import spray.http.StatusCode
import spray.http.StatusCodes
import spray.httpx.marshalling.Marshaller
import spray.httpx.marshalling.MetaMarshallers
import spray.json.JsValue
import spray.json.RootJsonFormat
import spray.routing.Directives
import spray.http.HttpHeaders.`Cache-Control`
import spray.http.CacheDirectives.`no-cache`
import org.trelev.eventmgr.domain._
import org.trelev.eventmgr.domain.EventActor._

class EventResource(event: ActorRef)(implicit executionContext: ExecutionContext)
  extends Directives with DefaultJsonFormats with CORSSupport with TokenAuthenticator {

  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = Timeout(2.seconds)

  case class EventBody(title: String, description: String, location: Location, host: Host, date: DateTime, duration: org.joda.time.Duration, maxParticipants: Int)
  case class UpdateEventBody(title: String, description: String, location: Location, host: Host, date: DateTime, duration: org.joda.time.Duration, maxParticipants: Int, informParticipants: Boolean)
  case class ShowEventBody(id: EventID, title: String, description: String, location: Location, firstName: String, lastName: String, date: DateTime, duration: org.joda.time.Duration, maxParticipants: Int, regParticipants: Int)
  implicit val evBodyFormat = jsonFormat7(EventBody)
  implicit val evUpdateBodyFormat = jsonFormat8(UpdateEventBody)
  implicit val evShowBodyFormat = jsonFormat10(ShowEventBody)

  implicit object EitherErrorSelector extends ErrorSelector[RegisterParticipantFailed.type] {
    def apply(v: RegisterParticipantFailed.type): StatusCode = { StatusCodes.BadRequest }
  }

  type PubEvent = (Event, Int)

  def consolidatePubEvent(ev: PubEvent) = {
    val (event, pcount) = ev
    ShowEventBody(event.id, event.title, event.description, event.location, event.host.firstName, event.host.lastName, event.date, event.duration, event.maxParticipants, pcount)
  }

  val route =
    cors {
      path("test") {
        get {
          complete {
            event ! "mail"
            "uaah"
          }
        } ~
          delete {
            complete { "del" }
          }
      } ~
        pathPrefix("organisation" / Segment / "events") { tenant =>
          pathEnd {
            get {
              respondWithHeader(`Cache-Control`(`no-cache`)) {
                complete {
                  (event ? ListEvents(TenantID(tenant))).mapTo[List[PubEvent]].map { list =>
                    list.map(consolidatePubEvent(_))
                  }
                }
              }
            } ~
              post {
                handleWith { ev: EventBody => (event ? CreateEvent(Event(null, TenantID(tenant), ev.title, ev.description, ev.host, ev.location, ev.date, ev.duration, ev.maxParticipants))).mapTo[Event] }
              }
          } ~
            pathPrefix(Segment) { id =>
              pathEnd {
                get {
                  authenticate(byToken(TenantID(tenant))) { tid =>
                    respondWithHeader(`Cache-Control`(`no-cache`)) {
                      complete((event ? ShowEvent(EventID(id))).mapTo[Option[Event]])
                    }
                  }
                } ~
                  put {
                    authenticate(byToken(TenantID(tenant))) { tid =>
                      handleWith { ev: UpdateEventBody => (event ? UpdateEvent(Event(EventID(id), tid, ev.title, ev.description, ev.host, ev.location, ev.date, ev.duration, ev.maxParticipants), ev.informParticipants)).mapTo[Option[Event]] }
                    }
                  } ~
                  delete {
                    authenticate(byToken(TenantID(tenant))) { tid =>
                      parameters('inform ? false) { inform =>
                        complete { (event ? CancelEvent(EventID(id), inform)).mapTo[Option[Ok.type]] }
                      }
                    }
                  }
              } ~
                pathPrefix("participants") {
                  pathEnd {
                    get {
                      authenticate(byToken(TenantID(tenant))) { tid =>
                        respondWithHeader(`Cache-Control`(`no-cache`)) {
                          complete((event ? ShowParticipants(EventID(id))).mapTo[Option[Seq[Participant]]])
                        }
                      }
                    }
                  }
                } ~
                path("participants" / Segment) { mail =>
                  /*unprotected as performed by the client widget when a new participant signs up*/
                  post {
                    entity(as[Participant]) { par =>
                      val resp = (event ? RegisterParticipant(EventID(id), par)).mapTo[Either[FailedCommand, PubEvent]]
                      onSuccess(resp)(_.fold(l => respondWithStatus(StatusCodes.BadRequest)(complete(l)),
                        r => complete(consolidatePubEvent(r))))
                    }
                  } ~
                    delete {
                      authenticate(byToken(TenantID(tenant))) { tid =>
                        onSuccess((event ? UnregisterParticipant(EventID(id), mail)).mapTo[Either[FailedCommand, Ok.type]]) { result =>
                          result.fold(l => respondWithStatus(if (l.code == 404) StatusCodes.NotFound else StatusCodes.BadRequest)(complete(l)),
                            r => complete(""))
                        }
                      }
                    }
                }

            }

        }
    } /* ~
    path("register" / "image") {
      post {
        handleWith { data: MultipartFormData =>
          data.fields.get("files[]") match {
            case Some(imageEntity) =>
              val size = imageEntity.entity.buffer.length
              println(s"Uploaded $size")
              ImageUploaded(size)
            case None =>
              println("No files")
              ImageUploaded(0)
          }
        }
      }
    }*/

}
