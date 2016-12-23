package nephtys.dualframe.cqrs.server.modules

import java.util.Date

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, get, onSuccess, parameters, path, redirect}
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
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
import org.nephtys.loom.generic.protocol.InternalStructures.EndpointRoot
import org.nephtys.loom.generic.protocol.{Backend, Protocol}
import upickle.default._
import nephty.oidc.helper.GoogleAuth
import nephty.oidc.helper.GoogleAuth.RemoteVerifiedIdentityToken
import nephtys.dualframe.cqrs.server.httphelper.RouteVerifier
import org.nephtys.loom.generic.protocol.InternalStructures.Email
import upickle.default.{write, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by nephtys on 12/18/16.
  */
class AuthModule extends AuthModulable {

  def port : Int = ConfigFactory.load().getInt("loom.charmserver.port")
  def pathToStaticResources : String =  ConfigFactory.load().getString("loom.charmserver.staticdirpath")
  def dummyAuthenticate : Option[Email] = Some(Email("dummy.user@dummy.world"))
  def certFileInsteadOfGeneratedWithFilePathAndPassword : Option[(String, String)] = ???

  def isHttps : Boolean = protocol.equals("https")
  def redirect_endpoint : String = ConfigFactory.load().getString("loom.charmserver.redirect_endpoint")
  def hostname : String = ConfigFactory.load().getString("loom.charmserver.raw_host")
  def protocol : String = ConfigFactory.load().getString("loom.charmserver.protocol")
  def completeurl : String = s"""$protocol://$hostname:$port"""
  def redirecturl : String = completeurl + "/"+redirect_endpoint



  private val google = new GoogleAuth(GoogleAuth.ClientCredentialFile(
    GoogleAuth.Web(
      ConfigFactory.load().getString("openidconnect.google.client.id"),
      ConfigFactory.load().getString("openidconnect.google.client.project_id"),
      ConfigFactory.load().getString("openidconnect.google.auth_uri"),
      ConfigFactory.load().getString("openidconnect.google.token_uri"),
      ConfigFactory.load().getString("openidconnect.google.auth_provider_x509_cert_url"),
      ConfigFactory.load().getString("openidconnect.google.client.secret")
    )
  ), GoogleAuth.RedirectFile(
    redirecturl,
    hostname,
    port
  ))



  def verify(authHeaderValue: String): Future[Option[Email]] = {
    if (authHeaderValue.startsWith("Bearer ")) {
      val token: String = authHeaderValue.substring("Bearer ".length).trim
      println(s"extracted token before remote verfiy : $token")
      verifyTokenRemote(token).map(verifiedtoken => {
        println("verified token:")
        verifiedtoken.foreach(println)
        verifiedtoken
          .filter(t => t.testIss) //this token comes from google
          .filter(t => t.aud.equals(google.clientCredentials.web.client_id)) //this token is for my application
          .filter(_.notExpired) //this token is not yet expired
          .map(s => Email(s.email))
      })
    } else {
      println("Header does not start with Bearer")
      Future.successful(None)
    }
  }

  protected def verifyTokenRemote(token: String): Future[Option[RemoteVerifiedIdentityToken]] = {
    Future {
      google.verify(token).toOption
    }
  }

  private val verifyUrl = ConfigFactory.load().getString("loom.charmserver.auth_prefix") / ConfigFactory.load().getString("loom.charmserver.verify_endpoint")

  def verifyRoute: Route = path(verifyUrl) {
    get {
      RouteVerifier.OIDCauthenticated {
        email => complete(write(email))
      }(this)
    }
  }



  def loginRoute : Route = {
    path(ConfigFactory.load().getString("loom.charmserver.auth_prefix") / ConfigFactory.load().getString("loom.charmserver.login_prefix") / ConfigFactory.load().getString("loom.charmserver.login_endpoint")) {
      get {
        redirect(google.authRequestUrl(), StatusCodes.TemporaryRedirect)
      }
    }
  }
  def redirectRoute : Route = {
    path(ConfigFactory.load().getString("loom.charmserver.redirect_endpoint")) {
      get {
        parameters('state, 'code
          , 'authuser, 'session_state, 'prompt
        ) {(state, code
            , authuser, session_state, prompt
           ) =>
          //TODO: extract state and code query parameters
          //TODO: exchange code for access and ID token via HTTPS POST (with client_id and client_secret)
          println("Received something at redirectedendpoint at " + new Date().toString)
          println(s"code = $code")
          println(s"state = $state")
          val tryaccess = google.postCodeForAccessAndIDToken(code)

          if (tryaccess.isSuccess) {
            val access = tryaccess.get
            println(access)
            onSuccess(verify("Bearer "+access.id_token)) { verified =>
              redirect("/index.html#/login/"+access.id_token, StatusCodes.TemporaryRedirect)
            }
          } else {
            complete {
              "There was an error validating your token, we are sorry about that. Try again"
            }
          }
        }
      }
    }
  }

}
