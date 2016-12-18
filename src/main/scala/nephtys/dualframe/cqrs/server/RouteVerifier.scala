package nephtys.dualframe.cqrs.server


import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, _}
import upickle.default._

import scala.util.{Failure, Success, Try}
import java.net.URI

import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import org.nephtys.loom.generic.protocol.InternalStructures.Email

/**
  * Created by nephtys on 12/18/16.
  */
object RouteVerifier {


  protected def XForwardedHostHeader = """X-Forwarded-Host"""

  protected def equalOiriginAndXForwardedHostHeader(seq: Seq[HttpHeader]): Boolean = {
    val origin = seq.find(_.is(OriginHeader.toLowerCase))
    val xforwardedhost = seq.find(_.is(XForwardedHostHeader.toLowerCase))
    if (origin.isDefined && xforwardedhost.isDefined) {
      val a = Try(new URI(origin.get.value().trim())).toOption
      val b = Try(new URI(xforwardedhost.get.value().trim())).toOption
      println(s"origin headers: $a vs $b")
      def defined = a.isDefined && b.isDefined
      def localhost = isLocalhost(a.get) && isLocalhost(b.get)
      def samehost = a.get.getHost.trim.equals(b.get.getHost.trim)
      defined && (localhost || samehost)
      //make java URI class deal with this for us
    } else {
      false
    }
  }

  protected def OriginHeader = """Origin"""

  protected def isLocalhost(uri: URI): Boolean = {
    println(s"checking localhost for uri $uri with ascii = ${uri.toASCIIString}")
    /* //this is wrong and commented out for that reason:
    if (uri.getHost != null) {
      println("host not null")
      false
    } else*/ if (uri.toASCIIString.startsWith("http://localhost:")) {
      println("startswith " + "http://localhost:")
      true
    } else if (uri.toASCIIString.startsWith("https://localhost:")) {
      println("startswith " + "https://localhost:")
      true
    } else if (uri.toASCIIString.startsWith("localhost:")) {
      println("startswith " + "localhost:")
      true
    } else {
      println("else")
      false
    }
  }

  protected def XRequestedWithHeader = """X-Requested-With"""

  protected def hasXRequestedWith(seq: Seq[HttpHeader]): Boolean = {
    seq.find(_.is(XRequestedWithHeader.toLowerCase())).exists(s => s.value().trim().toLowerCase()
      .equals(XRequestedWithValue.trim().toLowerCase()))
  }

  protected def XRequestedWithValue = """XMLHttpRequest"""

  //CSRF Protection:
  //compare Origin and X-FORWARDED-HOST headers for first CSRF Protection Stage
  //require "X-Requested-With: XMLHttpRequest" to guarantee same origin (as this is a custom header)
  def CSRFCheckForSameOrigin(route: =>Route): Route = {
    extractRequest { request => {
      if (!equalOiriginAndXForwardedHostHeader(request.headers)) {
        println("incoming post request not equalOiriginAndXForwardedHostHeader, see headers: " + request.headers)
        reject(MissingHeaderRejection(XForwardedHostHeader), MissingHeaderRejection(OriginHeader))
      } else if (!hasXRequestedWith(request.headers)) {
        println("incoming post request not hasXRequestedWith, see headers: " + request.headers)
        reject(MissingHeaderRejection(XRequestedWithHeader))
      } else {
        println("post request passed CSRF check")
        route
      }
    }
    }
  }
  import scala.concurrent.ExecutionContext.Implicits.global

  def OIDCauthenticated(route: Email => Route)(implicit authModule: AuthModulable): Route = {
    extractRequest { request => {
      request.headers.find(_.is("authorization")).map(_.value()).map(authheadervalue => {
        val r : Route = onComplete[Option[Email]](authModule.verify(authheadervalue)) {
          case Failure(e) => {
            println("Failed on OIDCAuthentication (verify failed)")
            reject(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge.apply("Bearer", Some("Exalted Charm Server"))))
          }
          case Success(None) => {
            println("Failed on OIDCAuthentication (verify returned None)")
            reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge.apply("Bearer", Some("Exalted Charm Server"))))
          }
          case Success(Some(email)) => route.apply(email)
        }
        r
      }).getOrElse({
        println("Failed on OIDCAuthentication (no header value)")
        val r : Route = reject(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge.apply("Bearer", Some("Exalted Charm Server"))))
        r
      })

    }
    }
  }

  def jsonPost[T](route : T => Route)(implicit reader : Reader[T]) : Route = {
    entity(as[String]) { jsonstring => {
      Try(read[T](jsonstring)) match {
        case Success(t) => route.apply(t)
        case Failure(e) => {
          println("jsonPost failed with error = " + e.toString)
          reject()
        }
      }
    }
    }
  }
}
