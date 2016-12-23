import java.net.{Inet6Address, InetAddress}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import nephtys.dualframe.cqrs.server.modules.{AuthModule, PersistenceModule}
import nephtys.dualframe.cqrs.server.ProtocolRouteDefinition
import nephtys.dualframe.cqrs.server.httphelper.CertManager
import nephtys.loom.protocol.vanilla.solar.{Solar, SolarProtocol}

/**
  * Created by nephtys on 12/18/16.
  */
object SimpleTestMain extends App {

  implicit val actorSystem = ActorSystem()
  implicit val actorMaterializer = ActorMaterializer()
  implicit val authModule = new AuthModule()
  implicit val persistenceModule = new PersistenceModule()

  val route : Route = path("help") {
    get {
      complete("This is the help path you were looking for")
    }
  } ~ authModule.loginRoute ~ authModule.redirectRoute ~ authModule.verifyRoute ~ pathSingleSlash {
    get {
      complete {
        "Hello world from loom server, use GET /{prefix}/aggregates to access commands, \nor " +
          "GET /auth/login/google to get an ID token via OpenID Connect" +
        "\nYou may also list all aggregates via GET /prefixes"+
        "\nVerify access tokens by calling GET /auth/verify"
      }
    }
  } ~  ProtocolRouteDefinition.createProtocolRoute[Solar, SolarProtocol.type](SolarProtocol) ~ ProtocolRouteDefinition.createIndex(SolarProtocol.endpointRoot)

  val hostname : String = authModule.hostname
  val port = authModule.port

  def hostip : String = InetAddress.getAllByName(hostname).filter(_.isInstanceOf[Inet6Address]).map(_.asInstanceOf[Inet6Address].getHostAddress).headOption.getOrElse("localhost")

  println(s"binding to IPv6 address: $hostip")
  import scala.concurrent.ExecutionContext.Implicits.global

  val usesHttps : Boolean = authModule.isHttps
  if (usesHttps) {
    println("using SSL cert...")
    val https = CertManager.buildSSLContextFrom()
    val x = Http().bindAndHandle(route,hostip,port, connectionContext = https)
    println("Opened at: " + hostip)
  } else {
    println("binding without SSL...")
    val x = Http().bindAndHandle(route,hostip,port)
    println("Opened at: " + hostip)
  }
}
