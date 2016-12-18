package nephtys.dualframe.cqrs.server.modules
import org.nephtys.loom.generic.protocol.Aggregate
import org.nephtys.loom.generic.protocol.InternalStructures.{Email, EndpointRoot}

import scala.concurrent.Future

/**
  * Created by nephtys on 12/18/16.
  */
class PersistenceModule extends PersistenceModulable{
//todo: implement actor hierarchy of persistent actors, and a query system with etag management and per user trees
  override def getAggregates[Agg <: Aggregate[Agg]](endpointRoot: EndpointRoot, requester: Email, includePublic: Boolean, includeReadOnly: Boolean): Future[Seq[Agg]] = Future.successful(Seq.empty)
}
