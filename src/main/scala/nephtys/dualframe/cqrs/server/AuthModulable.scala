package nephtys.dualframe.cqrs.server


import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
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
