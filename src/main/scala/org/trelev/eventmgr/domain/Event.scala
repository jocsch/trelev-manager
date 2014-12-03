package org.trelev.eventmgr.domain

import org.joda.time.DateTime
import org.joda.time.Duration

case class Location(city: String, place: String)

trait ID{val value: String}

case class EventID(value: String) extends ID

case class Event(id: EventID, tenant: TenantID, title: String, description: String,  host: Host, 
				 location: Location, date: DateTime, duration: Duration, maxParticipants: Int)
				