package org.trelev.eventmgr.domain

/**
 * A "typical" user value containing its identity, name and email.
 *
 * @param id the identity
 * @param firstName the first name
 * @param lastName the last name
 * @param email the email address
 */
case class Host(firstName: String, lastName: String, email: String, phone: String)

case class Entourage(children: Int, adults: Int)

case class Participant(firstName: String, lastName: String, email: String, entourage: Entourage)

case class TenantID(value: String) extends ID

case class Tenant(id: TenantID, password: String, ccmail: String)