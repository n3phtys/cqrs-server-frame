package nephtys.dualframe.cqrs.server.modules

import org.nephtys.loom.generic.protocol.{Backend, Protocol}
import org.nephtys.loom.generic.protocol.InternalStructures.{Email, EndpointRoot}

import scala.concurrent.Future

/**
  * Created by nephtys on 12/18/16.
  */
trait PersistenceModulable {

  def etag(endpointRoot: EndpointRoot) : akka.http.scaladsl.model.headers.EntityTag = akka.http.scaladsl.model.headers.EntityTag("abc", weak = false)

  def getAggregates[Agg <: org.nephtys.loom.generic.protocol.Aggregate[Agg]](endpointRoot: EndpointRoot, requester : Email, includePublic : Boolean, includeReadOnly : Boolean) : Future[Seq[Agg]]

  //TODO: implement generic DoCommands taking list of commands, email, and returning future of failablelist
}
