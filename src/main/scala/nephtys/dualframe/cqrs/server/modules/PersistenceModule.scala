package nephtys.dualframe.cqrs.server.modules
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.headers.EntityTag
import akka.stream.ActorMaterializer
import nephtys.dualframe.cqrs.server.modules.persistence.{PersistenceKeeper, QueryModule}
import org.nephtys.loom.generic.protocol.{Aggregate, Backend, Protocol}
import org.nephtys.loom.generic.protocol.InternalStructures.{Email, EndpointRoot, FailableList}

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}


/**
  * Created by nephtys on 12/18/16.
  */
class PersistenceModule(implicit val mat : ActorMaterializer, val system : ActorSystem) extends PersistenceModulable{

  val querymodule = new QueryModule()

  val persistenceActor = new ConcurrentHashMap[EndpointRoot, ActorRef]()



  override def getAggregates[Agg <: Aggregate[Agg]](endpointRoot: EndpointRoot, requester: Email, includePublic: Boolean, includeReadOnly: Boolean): Future[Seq[Agg]] = querymodule.getAggregates(endpointRoot, requester, includePublic, includeReadOnly)

  override def etag(endpointRoot: EndpointRoot): EntityTag =  querymodule.etag(endpointRoot)

  override def doCommands[T <: Aggregate[T], P <: Protocol[T] with Backend[T]](commands : Seq[P#Command], requester : Email, backend : P) : Future[FailableList[P#Event]] = {
    val p = commands.map(c => (c, Promise[Try[P#Event]])).toMap


    val f : java.util.function.Function[_ >: EndpointRoot, _ <: ActorRef] = new java.util.function.Function[EndpointRoot, ActorRef] {
      override def apply(v1: EndpointRoot): ActorRef = system.actorOf(PersistenceKeeper.props[T, P](v1.prefix))
    }

    val actorref = persistenceActor.computeIfAbsent(backend.endpointRoot, f)

    commands.foreach(command => {
      val promise : Promise[Try[P#Event]] = p(command)
      actorref ! PersistenceKeeper.Request[T,P](command, requester, promise)
    })

    Future.sequence(p.map(_._2.future)).map(s =>{
      val seq : Iterable[Either[P#Event, String]] = s.map(t => {
        val ei : Either[P#Event, String] = t match {
          case Success(suc) => Left(suc)
          case Failure(e) => Right(e.getMessage)
        }
        ei
      }
      )

      FailableList[P#Event](seq.toSeq)
    })
  }



}
