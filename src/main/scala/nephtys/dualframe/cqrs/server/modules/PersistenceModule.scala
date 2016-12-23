package nephtys.dualframe.cqrs.server.modules
import akka.http.scaladsl.model.headers.EntityTag
import org.nephtys.loom.generic.protocol.{Aggregate, Protocol}
import org.nephtys.loom.generic.protocol.InternalStructures.{Email, EndpointRoot, FailableList}

import scala.concurrent.Future

/**
  * Created by nephtys on 12/18/16.
  */
class PersistenceModule extends PersistenceModulable{
//todo: implement actor hierarchy of persistent actors, and a query system with etag management and per user trees


  //TODO: remove mock values:
  override def getAggregates[Agg <: Aggregate[Agg]](endpointRoot: EndpointRoot, requester: Email, includePublic: Boolean, includeReadOnly: Boolean): Future[Seq[Agg]] = Future.successful(Seq.empty)

  override def etag(endpointRoot: EndpointRoot): EntityTag =  akka.http.scaladsl.model.headers.EntityTag("abc", weak = false) //TODO: replace by real call

  override def doCommands[T <: Aggregate[T], P <: Protocol[T]](commands: Seq[P#Command], requester: Email): Future[FailableList[P#Event]] = Future.successful(FailableList(Seq.empty))

}
