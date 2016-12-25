package nephtys.dualframe.cqrs.server.modules.persistence

import akka.actor.{ActorRef, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import nephtys.dualframe.cqrs.server.modules.persistence.PersistenceKeeper.{MakeSnapshot, Request}
import org.nephtys.loom.generic.protocol.InternalStructures.{Email, ID}
import org.nephtys.loom.generic.protocol.{Backend, Protocol}

import scala.concurrent.Promise
import scala.util.{Failure, Success, Try}

class PersistenceKeeper[Agg <: org.nephtys.loom.generic.protocol.Aggregate[Agg], T <: Backend[Agg] with Protocol[Agg]](val persistenceId: String) extends PersistentActor{


  private var aggregate : Map[ID[Agg], Agg] = Map.empty

  override def receiveRecover: Receive = {
    case SnapshotOffer(metadate, snapshot: Map[_, _]) => {
      Try(snapshot.asInstanceOf[Map[ID[Agg], Agg]]) match {
        case Success(s) => {
          aggregate = s
        }
        case Failure(e) => {
          println(s"Found weird snapshot in LevelDB: $snapshot with error $e")
        }
      }
    }
    case e : T#Event => {
      aggregate = e.commit(aggregate)
    }
  }

  override def receiveCommand: Receive = {
    case req : PersistenceKeeper.Request[Agg, T]=> {
      val t : Try[T#Event] = req.command.validate(req.requester,aggregate)
      t match {
        case Success(event) => {
          persist(event)(ae => aggregate = event.commit(aggregate))
        }
        case _ =>
      }
      val promisedType = req.promise.success(t)
    }
    case MakeSnapshot => {
      saveSnapshot(aggregate)
    }
  }
}

object PersistenceKeeper {
  def props[Agg <: org.nephtys.loom.generic.protocol.Aggregate[Agg], T <: Backend[Agg] with Protocol[Agg]](persistenceId : String): Props = Props(new PersistenceKeeper[Agg, T](persistenceId))

  case class Request[Agg <: org.nephtys.loom.generic.protocol.Aggregate[Agg], T <: Backend[Agg] with Protocol[Agg]](command: T#Command, requester : Email, promise : Promise[Try[T#Event]])

  case object MakeSnapshot
}