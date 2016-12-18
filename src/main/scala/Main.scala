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

/**
  * Created by nephtys on 12/18/16.
  */
object Main extends App {

  implicit val actorSystem = ActorSystem()
  implicit val actorMaterializer = ActorMaterializer()

  val route : Route = pathSingleSlash {
    get {
      complete {
        "Hello world from loom server, use GET /{prefix}/aggregates to access commands, \nor " +
          "GET /auth/login/google to get an ID token via OpenID Connect" +
        "\nYou may also list all aggregates via GET /prefixes"+
        "\nVerify access tokens by calling GET /auth/verify"
      }
    }
  }

  val host : String = "localhost"
  val port = 8080
  Http().bindAndHandle(route,host,port)
  println(s"running server at http://$host:$port")
}
