package org.trelev.eventmgr.serializer

import org.trelev.eventmgr.domain.EventActor._
import org.trelev.eventmgr.web.DefaultJsonFormats
import akka.serialization.Serializer
import spray.json._
import org.trelev.eventmgr.domain.SerTest
import org.trelev.eventmgr.domain.EventActor.EventCreated


abstract class JsonSerializer[T] extends Serializer with DefaultJsonFormats {
  def includeManifest = true

  def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]) = {
     val source = new String(bytes, "UTF-8")
	   val json = source.parseJson
	   //json.convertTo[EventCreated]
	   //json.convertTo[T]
     convert(json)
  }
  
  def convert(value: JsValue): AnyRef 
  
  def toJson(value: T): JsValue
  
  def toBinary(x: AnyRef) = {
     //val ev = x.asInstanceOf[EventCreated]  
     val ev = x.asInstanceOf[T]  
     //ev.toJson.compactPrint.getBytes("UTF-8")
     toJson(ev).compactPrint.getBytes("UTF-8")
  }
}

class EventCreatedSerializer extends JsonSerializer[EventCreated] {
  def identifier = 37731
  implicit val format = jsonFormat1(EventCreated)
  def convert(value: JsValue) = value.convertTo[EventCreated] 
  def toJson(value: EventCreated) = value.toJson
}

class EventCanceledSerializer extends JsonSerializer[EventCanceled] {
  def identifier = 37732
  
  implicit val format = jsonFormat1(EventCanceled)
  def convert(value: JsValue) = value.convertTo[EventCanceled] 
  def toJson(value: EventCanceled) = value.toJson
}

class EventUpdatedSerializer extends JsonSerializer[EventUpdated] {
  def identifier = 37733
  
  implicit val format = jsonFormat1(EventUpdated)
  def convert(value: JsValue) = value.convertTo[EventUpdated] 
  def toJson(value: EventUpdated) = value.toJson
}

class ParticipantRegisteredSerializer extends JsonSerializer[ParticipantRegistered] {
  def identifier = 37734
  
  implicit val format = jsonFormat2(ParticipantRegistered)
  def convert(value: JsValue) = value.convertTo[ParticipantRegistered] 
  def toJson(value: ParticipantRegistered) = value.toJson
}

class ParticipantUnregisteredSerializer extends JsonSerializer[ParticipantUnregistered] {
  def identifier = 37735
  implicit val format = jsonFormat2(ParticipantUnregistered)
  def convert(value: JsValue) = value.convertTo[ParticipantUnregistered] 
  def toJson(value: ParticipantUnregistered) = value.toJson
}

class SerTestSer extends JsonSerializer[SerTest] {
  def identifier = 37736
  implicit val format = jsonFormat1(SerTest)
  def convert(value: JsValue) = value.convertTo[SerTest] 
  def toJson(value: SerTest) = value.toJson
}
