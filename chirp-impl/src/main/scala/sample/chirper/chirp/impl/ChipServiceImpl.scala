/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.chirp.impl

import java.time.Instant

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.slf4j.LoggerFactory

import com.datastax.driver.core.Row
import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry
import com.lightbend.lagom.javadsl.pubsub.TopicId

import akka.NotUsed
import akka.stream.javadsl.Source
import javax.inject.Inject
import sample.chirper.chirp.api.Chirp
import sample.chirper.chirp.api.ChirpService
import sample.chirper.chirp.api.HistoricalChirpsRequest
import sample.chirper.chirp.api.LiveChirpsRequest

object ChirpServiceImpl {
  final val MaxTopics = 1024
}

class ChirpServiceImpl @Inject()(
  topics: PubSubRegistry,
  db: CassandraSession
  )(implicit ex: ExecutionContext) extends ChirpService {

  // Needed to convert some Scala types to Java
  import converter.ServiceCallConverter._

  private val log = LoggerFactory.getLogger(classOf[ChirpServiceImpl])

  createTable()
  
  private def createTable(): Unit = {
    // @formatter:off
    val result = db.executeCreateTable(
      """CREATE TABLE IF NOT EXISTS chirp
       | (userId text, timestamp bigint, uuid text, message text,
       | PRIMARY KEY (userId, timestamp, uuid))
       |""".stripMargin
    )
    result.onFailure {
      case err: Throwable => log.error(s"Failed to create chirp table, due to: ${err.getMessage}", err)
    }
  }

  override def addChirp(userId: String): ServiceCall[Chirp, NotUsed] = {
    chirp => {
      if (userId != chirp.userId)
        throw new IllegalArgumentException(s"UserId ${userId} did not match userId in ${chirp}")

      val topic = topics.refFor(TopicId.of(classOf[Chirp], topicQualifier(userId)))
      topic.publish(chirp)

      db.executeWrite("INSERT INTO chirp (userId, uuid, timestamp, message) VALUES (?, ?, ?, ?)",
          chirp.userId, chirp.uuid, chirp.timestamp.toEpochMilli().asInstanceOf[AnyRef],
          chirp.message)
    }
  }

  private def topicQualifier(userId: String): String =
    String.valueOf(Math.abs(userId.hashCode()) % ChirpServiceImpl.MaxTopics)

  override def getLiveChirps(): ServiceCall[LiveChirpsRequest, Source[Chirp, _]] = {
    req => {
      recentChirps(req.userIds).map { chirps =>
        val sources = for(userId <- req.userIds) yield {
          val topic = topics.refFor(TopicId.of(classOf[Chirp], topicQualifier(userId)))
          topic.subscriber()
        }
        val users = req.userIds.toSet
        val publishedChirps = Source.from(sources.asJava).flatMapMerge(sources.size, x => x)
          .filter(chirp => users(chirp.userId))

        // We currently ignore the fact that it is possible to get duplicate chirps
        // from the recent and the topic. That can be solved with a de-duplication stage.
        Source.from(chirps.asJava).concat(publishedChirps)
      }
    }
  }

  override def  getHistoricalChirps(): ServiceCall[HistoricalChirpsRequest, Source[Chirp, _]] = {
    req => {
      val userIds = req.userIds
      val chirps = userIds.map { userId => 
        db.select("SELECT * FROM chirp WHERE userId = ? AND timestamp >= ? ORDER BY timestamp ASC",
                  userId, Long.box(req.fromTime.toEpochMilli())).map(mapChirp)
      }
      // Chirps from one user are ordered by timestamp, but chirps from different
      // users are not ordered. That can be improved by implementing a smarter
      // merge that takes the timestamps into account.
      val res = Source.from(chirps.asJava).flatMapMerge(chirps.size, x => x)
      Future.successful(res)
    }
  }

  private def mapChirp(row: Row): Chirp = {
    Chirp(row.getString("userId"), row.getString("message"), 
        Option(Instant.ofEpochMilli(row.getLong("timestamp"))), Option(row.getString("uuid")))
  }

  private def recentChirps(userIds: Seq[String]): Future[Seq[Chirp]] = {
    val limit = 10
    def getChirps(userId: String): Future[Seq[Chirp]] = {
      for {
        jRows <- db.selectAll("SELECT * FROM chirp WHERE userId = ? ORDER BY timestamp DESC LIMIT ?",
                             userId, Integer.valueOf(limit))
        rows = jRows.asScala.toVector
      } yield rows.map(mapChirp)
    }
    val results = Future.sequence(userIds.map(getChirps)).map(_.flatten)
    val sortedLimited = results.map(_.sorted.reverse) // reverse order

    sortedLimited.map(_.take(limit)) // take only latest chirps
  }

}
