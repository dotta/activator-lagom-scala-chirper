/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.activity.impl

import java.util.concurrent.TimeUnit.SECONDS

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.concurrent.Future

import org.junit.Assert.assertEquals
import org.junit.Test

import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.NotFound
import com.lightbend.lagom.javadsl.testkit.ServiceTest.bind
import com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup
import com.lightbend.lagom.javadsl.testkit.ServiceTest.withServer

import akka.NotUsed
import akka.stream.javadsl.Source
import akka.stream.testkit.javadsl.TestSink
import sample.chirper.activity.api.ActivityStreamService
import sample.chirper.activity.impl.ActivityStreamServiceTest.ChirpServiceStub
import sample.chirper.activity.impl.ActivityStreamServiceTest.FriendServiceStub
import sample.chirper.chirp.api.Chirp
import sample.chirper.chirp.api.ChirpService
import sample.chirper.chirp.api.HistoricalChirpsRequest
import sample.chirper.chirp.api.LiveChirpsRequest
import sample.chirper.friend.api.FriendId
import sample.chirper.friend.api.FriendService
import sample.chirper.friend.api.User

class ActivityStreamServiceTest {
  private val setup = defaultSetup.withCluster(false)
    .withConfigureBuilder(b => b.overrides(bind(classOf[FriendService]).to(classOf[FriendServiceStub]),
      bind(classOf[ChirpService]).to(classOf[ChirpServiceStub])))

  @throws(classOf[Exception])
  @Test
  def shouldGetLiveFeed(): Unit = {
    withServer(setup, server => {
      val feedService = server.client(classOf[ActivityStreamService])
      val chirps = feedService.getLiveActivityStream("usr1").invoke()
        .toCompletableFuture().get(3, SECONDS)
      val probe = chirps.runWith(TestSink.probe(server.system), server.materializer)
      probe.request(10)
      assertEquals("msg1", probe.expectNext().message)
      assertEquals("msg2", probe.expectNext().message)
      probe.cancel()
    })
  }

  @throws(classOf[Exception])
  @Test
  def shouldGetHistoricalFeed(): Unit = {
    withServer(setup, server => {
      val feedService = server.client(classOf[ActivityStreamService])
      val chirps = feedService.getHistoricalActivityStream("usr1").invoke()
        .toCompletableFuture().get(3, SECONDS)
      val probe = chirps.runWith(TestSink.probe(server.system), server.materializer)
      probe.request(10)
      assertEquals("msg1", probe.expectNext().message)
      probe.expectComplete()
    })
  }
}

object ActivityStreamServiceTest {
  private class FriendServiceStub extends FriendService {
    // Needed to convert some Scala types to Java
    import converter.ServiceCallConverter._


    private val usr1 = User("usr1", "User 1", Seq("usr2"))
    private val usr2 = new User("usr2", "User 2")

    override def getUser(id: String): ServiceCall[NotUsed, User] = {
      req =>
        {
          if (id == usr1.userId) Future.successful(usr1)
          else if (id == usr2.userId) Future.successful(usr2)
          else throw new NotFound(id)
        }
    }

    override def createUser(): ServiceCall[User, NotUsed] =
      _ => Future.successful(NotUsed)

    override def addFriend(userId: String): ServiceCall[FriendId, NotUsed] =
      _ => Future.successful(NotUsed)

    override def getFollowers(userId: String): ServiceCall[NotUsed, Seq[String]] = {
      _ =>
        {
          if (userId == usr1.userId) Future.successful(Seq.empty)
          else if (userId == usr2.userId) Future.successful(Seq("usr1"))
          else throw new NotFound(userId);
        }
    }
  }

  private class ChirpServiceStub extends ChirpService {
    // Needed to convert some Scala types to Java
    import converter.ServiceCallConverter._

    override def addChirp(userId: String): ServiceCall[Chirp, NotUsed] =
      _ => Future.successful(NotUsed)

    override def getLiveChirps(): ServiceCall[LiveChirpsRequest, Source[Chirp, _]] = {
      req =>
        {
          if (req.userIds.contains("usr2")) {
            val c1 = new Chirp("usr2", "msg1")
            val c2 = new Chirp("usr2", "msg2")
            Future.successful(Source.from(Seq(c1, c2).asJava))
          } else Future.successful(Source.empty())
        }
    }

    override def getHistoricalChirps(): ServiceCall[HistoricalChirpsRequest, Source[Chirp, _]] = {
      req =>
        {
          if (req.userIds.contains("usr2")) {
            val c1 = new Chirp("usr2", "msg1");
            Future.successful(Source.single(c1));
          } else
            Future.successful(Source.empty());
        }
    }

  }
}