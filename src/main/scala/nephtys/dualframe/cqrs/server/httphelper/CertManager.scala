package nephtys.dualframe.cqrs.server.httphelper

import java.io.{File, FileInputStream, InputStream}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import com.typesafe.config.ConfigFactory

/**
  * Created by nephtys on 12/18/16.
  */
object CertManager {

  def buildSSLContextFrom() : HttpsConnectionContext = {
    val keystoreFilepath = ConfigFactory.load().getString("loom.charmserver.certpath")

    val keystorePassword: Array[Char] = ConfigFactory.load().getString("loom.charmserver.certpassword").toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    println("opening stream from File = " + new File(keystoreFilepath).getAbsolutePath)
    val keystore: InputStream = {
      new FileInputStream(keystoreFilepath)
    }

    println("stream opened")
    ks.load(keystore, keystorePassword)

    println("stream loaded")
    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, keystorePassword)

    println("key factory initaited")
    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    println("trust factory")
    tmf.init(ks)
    println("trust factory initialized")

    //keystore.close()

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom())

    println("ssl context initated")
    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)


    println("context created")

    https
  }
}
