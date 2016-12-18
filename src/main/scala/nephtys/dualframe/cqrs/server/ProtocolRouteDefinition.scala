package nephtys.dualframe.cqrs.server


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import org.nephtys.loom.generic.protocol.InternalStructures.EndpointRoot
import org.nephtys.loom.generic.protocol.{Backend, Protocol}

/**
  * Created by nephtys on 12/18/16.
  */
object ProtocolRouteDefinition {

  //TODO:
  //auth GET login
  //auth GET verify
  //auth GET redirect
  //GET prefixes
  //per prefix: GET aggregates (with optional public parameter and optional read-only parameter)
  //per prefix: POST commands


  def createProtocolRoute[Agg <: org.nephtys.loom.generic.protocol.Aggregate[Agg], T <: Backend[Agg] with Protocol[Agg]](backend : T) : Route = ???


  def createIndex(seq : Seq[EndpointRoot]) : Route = {
    ???
  }
}
