package nephtys.dualframe.cqrs.server.modules.persistence


import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.headers.EntityTag
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.nephtys.loom.generic.protocol.{Aggregate, Backend, Protocol}
import org.nephtys.loom.generic.protocol.InternalStructures.{Email, EndpointRoot, ID}

import scala.collection.immutable.{SortedSet, TreeSet}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by nephtys on 12/25/16.
  */
class QueryModule(implicit val mat : ActorMaterializer, val system : ActorSystem) {

  //TODO: more efficient data structures (constant time access, linear time update), etags per requester, and so on


  def etag[Agg <: org.nephtys.loom.generic.protocol.Aggregate[Agg], T <: Backend[Agg] with Protocol[Agg]](endpointRoot: EndpointRoot): EntityTag = {
    val f : java.util.function.Function[_ >: EndpointRoot, _ <: (AtomicReference[Map[ID[_], Any]], AtomicLong, AtomicReference[EntityTag])] = genBuilderFunction[Agg, T](endpointRoot.prefix)
    aggregates.computeIfAbsent(endpointRoot, f)._3.get()
  }

  case class Pack[T](sequencenumber : Long, event: T)
  private def toPack[T](e : EventEnvelope) : Pack[T] = Pack(e.sequenceNr, e.event.asInstanceOf[T])



  private def genBuilderFunction[Agg <: org.nephtys.loom.generic.protocol.Aggregate[Agg], T <: Backend[Agg] with Protocol[Agg]](perstID : String) : java.util.function.Function[_ >: EndpointRoot, _ <: (AtomicReference[Map[ID[_], Any]], AtomicLong, AtomicReference[EntityTag])] = new java.util.function.Function[EndpointRoot, (AtomicReference[Map[ID[_], Any]], AtomicLong, AtomicReference[EntityTag])] {
    override def apply(v1: EndpointRoot): (AtomicReference[Map[ID[_], Any]], AtomicLong, AtomicReference[EntityTag]) = {
      //create empty states and bind to persistence query stream based on given persistenceid
      val r = (new AtomicReference[Map[ID[_], Any]](Map.empty), new AtomicLong(0), new AtomicReference[EntityTag](entityTagFromLong(0)))
      //connect to eventstream:
      val src: Source[EventEnvelope, NotUsed] = queries.eventsByPersistenceId(perstID, 0L, Long.MaxValue)
      val events : Source[Pack[T#Event], NotUsed] = src.filter(_.event.isInstanceOf[T#Event]).map(e => toPack[T#Event](e))
      events.runForeach(pack => {
        r.synchronized {
          val old : Map[ID[Agg], Agg] = r._1.get().asInstanceOf[Map[ID[Agg], Agg]]
          val ne : Map[ID[Agg], Agg] = pack.event.commit(old)
          val nef : Map[ID[_], Any] = ne.asInstanceOf[Map[ID[_], Any]]
          r._1.set(nef)
          r._2.set(pack.sequencenumber)
          r._3.set(entityTagFromLong(pack.sequencenumber))
        }
      })
      //return pair
      r
    }
  }

  def getAggregates[Agg <: org.nephtys.loom.generic.protocol.Aggregate[Agg], T <: Backend[Agg] with Protocol[Agg]](endpointRoot: EndpointRoot, requester: Email, includePublic: Boolean, includeReadOnly: Boolean): Future[Seq[Agg]] = {
    val f : java.util.function.Function[_ >: EndpointRoot, _ <: (AtomicReference[Map[ID[_], Any]], AtomicLong, AtomicReference[EntityTag])] = genBuilderFunction[Agg, T](endpointRoot.prefix)

    val (map, sequenceNr, entityTag) = aggregates.computeIfAbsent(endpointRoot, f)
    Future {
      val all = map.get()
      all.values.toSeq.map(_.asInstanceOf[Agg]).filter(a => (includePublic && a.public) || (includeReadOnly && a.readers.contains(requester)) || a.owner.equals(requester) ) //N*M runtime, too much!
    }
  }


  private val aggregates : ConcurrentHashMap[EndpointRoot, (AtomicReference[Map[ID[_], Any]], AtomicLong, AtomicReference[EntityTag])] = new ConcurrentHashMap[EndpointRoot, (AtomicReference[Map[ID[_], Any]], AtomicLong, AtomicReference[EntityTag])]()

  private lazy val queries = PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)

  private def entityTagFromLong(long : Long) : EntityTag = EntityTag(long.toString, weak = false)


  //TODO: implement snapshot on query side too
}
