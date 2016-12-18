package nephtys.dualframe.cqrs.server.modules

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.nephtys.loom.generic.protocol.InternalStructures.Email

import scala.concurrent.Future

/**
  * Created by nephtys on 12/18/16.
  */
trait AuthModulable {

  def loginRoute : Route
  def redirectRoute : Route
  def verifyRoute : Route

  def authRoutes : Route = loginRoute ~ redirectRoute ~ verifyRoute

  def verify(authHeaderValue : String) : Future[Option[Email]]

}
