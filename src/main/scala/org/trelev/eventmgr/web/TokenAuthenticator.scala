package org.trelev.eventmgr.web

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.typesafe.config.ConfigFactory

import spray.http.HttpHeader
import spray.routing.AuthenticationFailedRejection
import spray.routing.RequestContext
import spray.routing.{AuthenticationFailedRejection, RequestContext}
import spray.routing.authentication.{Authentication, ContextAuthenticator}
import org.trelev.eventmgr.domain.TenantID


trait TokenAuthenticator {

 val config = ConfigFactory.load()
 
  def byToken(id: TenantID): ContextAuthenticator[TenantID] = {
    ctx =>
      println(s"check token for $id ->" + ctx.request.headers)
      val header = ctx.request.headers.find(_.name == "Authorization")
      if (header isDefined) {
        doAuth(id, header.get)
      }
      else {
        Future(Left(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, List())))
      }
  }
  
   def doAuth(id: TenantID, header: HttpHeader): Future[Authentication[TenantID]] = {
    //TODO be flexibel in this instead of hardcoding to the first value
    val tenant = config.getConfig("trelev").getConfigList("tenants").get(0)
    val token = tenant.getString("apitoken")
    
    println(s"compare '$token' with " + header.value + " Result = " + token.equals(header.value))
    Future {
      Either.cond(token.equals(header.value),
        id,
        AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
    }

  }
}