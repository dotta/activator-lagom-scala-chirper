/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.load.impl

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import play.Logger;
import play.Logger.ALogger;
import sample.chirper.activity.api.ActivityStreamService;
import sample.chirper.chirp.api.Chirp;
import sample.chirper.chirp.api.ChirpService;
import sample.chirper.friend.api.FriendId;
import sample.chirper.friend.api.FriendService;
import sample.chirper.friend.api.User;
import sample.chirper.load.api.LoadTestService;
import sample.chirper.load.api.TestParams;
import scala.concurrent.duration.FiniteDuration;

import akka.japi.Pair;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.slf4j.LoggerFactory
import scala.concurrent.Future
import sample.chirper.load.api.TestParams
import scala.collection.JavaConverters._
import sample.chirper.load.api.LoadTestService
import sample.chirper.activity.api.ActivityStreamService
import sample.chirper.chirp.api.ChirpService
import sample.chirper.friend.api.FriendService
import sample.chirper.load.api.TestParams

class LoadTestServiceImpl @Inject() (
    chirpService: ChirpService,
    friendService: FriendService,
    activityService: ActivityStreamService)(implicit mat: Materializer) extends LoadTestService {
  // Needed to convert some Scala types to Java
  import converter.ServiceCallConverter._
    
  private val log = LoggerFactory.getLogger(classOf[LoadTestServiceImpl])

  // to create "unique" user ids we prefix them with this, convenient
  // to not have overlapping user ids when running in dev mode
  private val runSeq = new AtomicLong((System.currentTimeMillis()
    - LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()) / 1000)

  override def startLoad(): ServiceCall[NotUsed, Source[String, _]] = {
    _ => Future.successful(load(new TestParams()))
  }

  override def startLoadHeadless(): ServiceCall[TestParams, NotUsed] = {
    params =>
      {
        load(params).runWith(Sink.ignore(), mat)
        Future.successful(NotUsed)
      }
  }

  private def load(params: TestParams): Source[String, _] = {
    val runSeqNr = runSeq.incrementAndGet()
    val userIdPrefix = params.userIdPrefix.getOrElse(s"user-$runSeqNr-")

    log.info(s"Starting load with parameters: $params, users prefixed with: $userIdPrefix")
    val userNumbers = Source.range(1, params.users)
    val users = userNumbers.map(n => new User(userIdPrefix + n, userIdPrefix.toUpperCase() + n))
    val createdUsers = users
      .mapAsync(params.parallelism, user => friendService.createUser().invoke(user))
      .via(summary("created users"))

    val friendPairs = userNumbers.mapConcat(n => {
      (1 to params.friends).map(i => n -> (n + i)).asJava
    })

    val chirpCount = new AtomicLong()
    val addedFriends = friendPairs.mapAsyncUnordered(params.parallelism, pair => {
      val invoked = friendService.addFriend(userIdPrefix + pair._1).invoke(FriendId(userIdPrefix + pair._2))
      // start clients when last friend association has been created
      if (params.users == pair._1 && (params.users + params.friends) == pair._2)
        invoked.thenAccept(a => startClients(params.clients, userIdPrefix, chirpCount, runSeqNr))
      invoked
    }).via(summary("added friends"));

    val chirpNumbers = Source.range(1, params.chirps);
    val chirps = chirpNumbers.map(n => {
      val userId = userIdPrefix + (n % params.users);
      val message = "Hello " + n + " from " + userId;
      new Chirp(userId, message);
    })

    val postedChirps = chirps.mapAsyncUnordered(params.parallelism, chirp => {
      chirpService.addChirp(chirp.userId).invoke(chirp)
    }).via(summary("posted chirp"));

    val writes = Source.from(Arrays.asList(createdUsers, addedFriends, postedChirps))
      .flatMapConcat(s => s);

    val interval = FiniteDuration(5, TimeUnit.SECONDS)
    val clientsThroughput = Source.tick(interval, interval, "tick")
      .scan(new Throughput(System.nanoTime(), System.nanoTime(), 0, 0))((t, tick) => {
        val now = System.nanoTime()
        val totalCount = chirpCount.get()
        val count = totalCount - t.totalCount
        new Throughput(t.endTime, now, count, totalCount)
      })
      .filter(_.throughput > 0.0)
      .map(t => s"client throughput ${String.format("%.2f", Double.box(t.throughput))} chirps/s from " +
        s"${params.clients}  clients (total consumed: ${t.totalCount} chirps)")

    val output = Source.from(Arrays.asList(writes, clientsThroughput))
      .flatMapMerge(2, s => s)
      .map(s => {
        log.info(s)
        s
      }).map(s => {
        if (runSeq.get() != runSeqNr) {
          val msg = "New test started, stopping previous"
          log.info(msg)
          throw new RuntimeException(msg)
        }
        s
      })

    output;
  }

  private def summary(title: String): Flow[NotUsed, String, _] = {
    Flow.of(classOf[NotUsed])
      .scan(0)((count, elem) => count + 1)
      .drop(1)
      .groupedWithin(1000, FiniteDuration(1, TimeUnit.SECONDS))
      .map(list => list.get(list.size() - 1))
      .map(c => title + ": " + c)
  }

  private def startClients(numberOfClients: Int, userIdPrefix: String, chirpCount: AtomicLong, runSeqNr: Long): Unit = {
    log.info("starting " + numberOfClients + " clients for users prefixed with " + userIdPrefix)
    for (n <- 1 to numberOfClients) {
      activityService.getLiveActivityStream(userIdPrefix + n).invoke().thenAccept(src => {
        src.map(chirp =>
          if (runSeq.get() != runSeqNr) throw new RuntimeException("New test started, stopping previous clients")
          else chirp
        ).runForeach(chirp => chirpCount.incrementAndGet(), mat)
      })
    }
  }
}
