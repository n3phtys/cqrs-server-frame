package nephtys.dualframe.cqrs.server


import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.Directives.{conditional, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import nephtys.dualframe.cqrs.server.modules.{AuthModulable, PersistenceModulable}
import org.nephtys.loom.generic.protocol.InternalStructures.EndpointRoot
import org.nephtys.loom.generic.protocol.{Backend, Protocol}
import upickle.default._

import scala.util.{Failure, Success, Try}

/**
  * Created by nephtys on 12/18/16.
  */
object ProtocolRouteDefinition {




  //TODO: per prefix: POST commands


  import nephtys.dualframe.cqrs.server.httphelper.RouteVerifier._
  def createProtocolRoute[Agg <: org.nephtys.loom.generic.protocol.Aggregate[Agg], T <: Backend[Agg] with Protocol[Agg]](backend : T)(implicit authModulable: AuthModulable, persistenceModulable: PersistenceModulable) : Route = {
    val prefix : String = backend.endpointRoot.prefix
    def r1 : Route = path(prefix) {
      get {
        complete(s"hello from ${backend.endpointRoot.prefix}")
      }
    }
    def r2 : Route = path(prefix / "aggregates") {
      get {
        OIDCauthenticated { email => conditional(persistenceModulable.etag(backend.endpointRoot)){
          println(s"receiving something on /charms-GET by email = $email at date ${new Date()}")
          parameters('showpublic.?, 'showreadonly.?) { (showpublic : Option[String], showreadonly : Option[String]) =>
            onSuccess(persistenceModulable.getAggregates[Agg](backend.endpointRoot, email, showpublic.forall(_.equals(write(true))), showreadonly.forall(_.equals(write(true))))) { s =>
              complete(backend.writeAggregates(s))
            }
          }
        }
        }
      }
    }
    def r3 : Route = path(prefix / "commands") {
      post {
        CSRFCheckForSameOrigin {
          OIDCauthenticated { email =>
            entity(as[String]) { jsonstring => {
              Try(backend.readCommands(json = jsonstring)) match {
                case Failure(e) => reject()
                case Success(t) => complete("success parsing the incoming json commands") //todo: call internal persistence layer
              }
            }
            }
          }
        }
      }
    }

    val c : Route = r2 ~ r3 ~ r1
    c
  }


  def createIndex(seq : EndpointRoot*) : Route = {
    val list = write(seq)
    path("prefixes") {
      get {
        complete(list)
      }
    }
  }
}
