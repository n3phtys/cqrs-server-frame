package nephtys.dualframe.cqrs.server.modules

import org.nephtys.loom.generic.protocol.{Aggregate, Backend, Protocol}
import org.nephtys.loom.generic.protocol.InternalStructures.{Email, EndpointRoot, FailableList}

import scala.concurrent.Future

/**
  * Created by nephtys on 12/18/16.
  */
trait PersistenceModulable {

  def etag(endpointRoot: EndpointRoot) : akka.http.scaladsl.model.headers.EntityTag

  def getAggregates[Agg <: org.nephtys.loom.generic.protocol.Aggregate[Agg]](endpointRoot: EndpointRoot, requester : Email, includePublic : Boolean, includeReadOnly : Boolean) : Future[Seq[Agg]]

  //TODO: implement generic DoCommands taking list of commands, email, and returning future of failablelist

  def doCommands[T <: Aggregate[T], P <: Protocol[T]](commands : P#Command) : Future[FailableList[P#Event]]
}
