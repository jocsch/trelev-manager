package org.trelev.eventmgr.web

import java.util.UUID
import scala.reflect.ClassTag
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import spray.http.StatusCode
import spray.httpx.SprayJsonSupport
import spray.httpx.marshalling.CollectingMarshallingContext
import spray.httpx.marshalling.Marshaller
import spray.httpx.marshalling.MetaMarshallers
import spray.json._
import org.trelev.eventmgr.domain.Ok
import org.trelev.eventmgr.domain.FailedCommand
import org.trelev.eventmgr.domain.Entourage
import org.trelev.eventmgr.domain.TenantID
import org.trelev.eventmgr.domain.Tenant
import org.trelev.eventmgr.domain.FailedCommand
import org.trelev.eventmgr.domain.Ok
import org.trelev.eventmgr.domain.EventID
import org.trelev.eventmgr.domain.Participant
import org.trelev.eventmgr.domain.Location
import org.trelev.eventmgr.domain.Event
import org.trelev.eventmgr.domain.Host

/**
 * Contains useful JSON formats: ``j.u.Date``, ``j.u.UUID`` and others; it is useful
 * when creating traits that contain the ``JsonReader`` and ``JsonWriter`` instances
 * for types that contain ``Date``s, ``UUID``s and such like.
 */
trait DefaultJsonFormats extends DefaultJsonProtocol with SprayJsonSupport with MetaMarshallers {

  /**
   * Computes ``RootJsonFormat`` for type ``A`` if ``A`` is object
   */
  def jsonObjectFormat[A : ClassTag]: RootJsonFormat[A] = new RootJsonFormat[A] {
    val ct = implicitly[ClassTag[A]]
    def write(obj: A): JsValue = JsObject("value" -> JsString(ct.runtimeClass.getSimpleName))
    def read(json: JsValue): A = ct.runtimeClass.newInstance().asInstanceOf[A]
  }

  /**
   * Instance of the ``RootJsonFormat`` for the ``j.u.UUID``
   */
  implicit object UuidJsonFormat extends RootJsonFormat[UUID] {
    def write(x: UUID) = JsString(x.toString)
    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case x           => deserializationError("Expected UUID as JsString, but got " + x)
    }
  }
  
  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    private val parserISO : DateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();
    override def write(obj: DateTime) = JsString(parserISO.print(obj))
    override def read(json: JsValue) : DateTime = json match {
      case JsString(s) => parserISO.parseDateTime(s)
      case x => deserializationError("Expected 8061 date, but got " + x)
    }
  }
  
  implicit object DurationJsonFormat extends RootJsonFormat[Duration] {
    override def write(obj: Duration) = JsNumber(obj.getStandardMinutes())
    override def read(json: JsValue) : Duration = json match {
      case JsNumber(s) => Duration.standardMinutes(s.toLong)
      case x => deserializationError("Expected duration in minutes, but got " + x)
    }
  }
  
  implicit object EventIDFormat extends RootJsonFormat[EventID] {
    def write(x: EventID) = JsString(x.value)
    def read(value: JsValue) = value match {
      case JsString(x) => EventID(x)
      case x           => deserializationError("Expected EventID as JsString, but got " + x)
    }
  }
  
  implicit object TenantIDFormat extends RootJsonFormat[TenantID] {
    def write(x: TenantID) = JsString(x.value)
    def read(value: JsValue) = value match {
      case JsString(x) => TenantID(x)
      case x           => deserializationError("Expected TenantID as JsString, but got " + x)
    }
  }
  
  implicit object FailedCommandFormat extends RootJsonFormat[FailedCommand] {
    def write(x: FailedCommand) = JsObject("reason"->JsString(x.reason), "code"->JsNumber(x.code))
    def read(value: JsValue) = value match {
      case x           => deserializationError("Can not send a FailedCommand: " + x)
    }
  }
  
  implicit val tenantFormat = jsonFormat3(Tenant)
  implicit val locationFormat = jsonFormat2(Location)
  implicit val hostFormat = jsonFormat4(Host)
  implicit val eventFormat = jsonFormat9(Event)
  implicit val entourageFormat = jsonFormat2(Entourage)
  implicit val participantFormat = jsonFormat4(Participant)
  implicit val okFormat = jsonObjectFormat[Ok.type]

  /**
   * Type alias for function that converts ``A`` to some ``StatusCode``
   * @tparam A the type of the input values
   */
  type ErrorSelector[A] = A => StatusCode

  /**
   * Marshals instances of ``Either[A, B]`` into appropriate HTTP responses by marshalling the values
   * in the left or right projections; and by selecting the appropriate HTTP status code for the
   * values in the left projection.
   *
   * @param ma marshaller for the left projection
   * @param mb marshaller for the right projection
   * @param esa the selector converting the left projection to HTTP status code
   * @tparam A the left projection
   * @tparam B the right projection
   * @return marshaller
   */
  implicit def errorSelectingEitherMarshaller[A, B](implicit ma: Marshaller[A], mb: Marshaller[B], esa: ErrorSelector[A]): Marshaller[Either[A, B]] =
    Marshaller[Either[A, B]] { (value, ctx) =>
      print("eeeee")
      value match {
        case Left(a) =>
          val mc = new CollectingMarshallingContext()
          ma(a, mc)
          ctx.handleError(ErrorResponseException(esa(a), mc.entity))
        case Right(b) =>
          mb(b, ctx)
      }
    }

}
