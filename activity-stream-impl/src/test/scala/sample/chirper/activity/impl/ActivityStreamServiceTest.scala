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
import converter.ServiceCallConverter._
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
      val chirps = feedService.getLiveActivityStream().invoke("usr1", NotUsed)
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
      val chirps = feedService.getHistoricalActivityStream().invoke("usr1", NotUsed)
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

    private val usr1 = User("usr1", "User 1", Seq("usr2"))
    private val usr2 = User("usr2", "User 2")

    override def getUser(): ServiceCall[String, NotUsed, User] = {
      (id, req) =>
        {
          if (id == usr1.userId) Future.successful(usr1)
          else if (id == usr2.userId) Future.successful(usr2)
          else throw new NotFound(id)
        }
    }

    override def createUser(): ServiceCall[NotUsed, User, NotUsed] =
      (id, req) => Future.successful(NotUsed)

    override def addFriend(): ServiceCall[String, FriendId, NotUsed] =
      (id, req) => Future.successful(NotUsed)

    override def getFollowers(): ServiceCall[String, NotUsed, Seq[String]] = {
      (id, req) =>
        {
          if (id == usr1.userId) Future.successful(Seq.empty)
          else if (id == usr2.userId) Future.successful(Seq("usr1"))
          else throw new NotFound(id);
        }
    }
  }

  private class ChirpServiceStub extends ChirpService {

    override def addChirp(): ServiceCall[String, Chirp, NotUsed] =
      (id, req) => Future.successful(NotUsed)

    override def getLiveChirps(): ServiceCall[NotUsed, LiveChirpsRequest, Source[Chirp, _]] = {
      (id, req) =>
        {
          if (req.userIds.contains("usr2")) {
            val c1 = new Chirp("usr2", "msg1")
            val c2 = new Chirp("usr2", "msg2")
            Future.successful(Source.from(Seq(c1, c2).asJava))
          } else Future.successful(Source.empty())
        }
    }

    override def getHistoricalChirps(): ServiceCall[NotUsed, HistoricalChirpsRequest, Source[Chirp, _]] = {
      (id, req) =>
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