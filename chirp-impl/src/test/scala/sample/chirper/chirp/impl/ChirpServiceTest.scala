/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.chirp.impl

import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS

import scala.collection.immutable.Seq

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.lightbend.lagom.javadsl.testkit.ServiceTest.TestServer
import com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup
import com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer

import akka.stream.testkit.javadsl.TestSink
import sample.chirper.chirp.api.Chirp
import sample.chirper.chirp.api.ChirpService
import sample.chirper.chirp.api.HistoricalChirpsRequest
import sample.chirper.chirp.api.LiveChirpsRequest

object ChirpServiceTest {
  var server: TestServer = _
  @BeforeClass
  def setUp(): Unit = {
    server = startServer(defaultSetup)
  }

  @AfterClass
  def tearDown(): Unit = {
    server.stop()
    server = null
  }
}
class ChirpServiceTest {
  import ChirpServiceTest.server


  @throws(classOf[Exception])
  @Test
  def shouldPublishShirpsToSubscribers(): Unit = {
    val chirpService = server.client(classOf[ChirpService])
    val request = new LiveChirpsRequest(Seq("usr1", "usr2"))
    val chirps1 = chirpService.getLiveChirps().invoke(request).toCompletableFuture().get(3, SECONDS)
    val probe1 = chirps1.runWith(TestSink.probe(server.system), server.materializer)
    probe1.request(10)
    val chirps2 = chirpService.getLiveChirps().invoke(request).toCompletableFuture().get(3, SECONDS)
    val probe2 = chirps2.runWith(TestSink.probe(server.system), server.materializer)
    probe2.request(10)

    val chirp1 = new Chirp("usr1", "hello 1")
    chirpService.addChirp("usr1").invoke(chirp1).toCompletableFuture().get(3, SECONDS)
    probe1.expectNext(chirp1)
    probe2.expectNext(chirp1)

    val chirp2 = new Chirp("usr1", "hello 2")
    chirpService.addChirp("usr1").invoke(chirp2).toCompletableFuture().get(3, SECONDS)
    probe1.expectNext(chirp2)
    probe2.expectNext(chirp2)

    val chirp3 = new Chirp("usr2", "hello 3");
    chirpService.addChirp("usr2").invoke(chirp3).toCompletableFuture().get(3, SECONDS)
    probe1.expectNext(chirp3)
    probe2.expectNext(chirp3)

    probe1.cancel()
    probe2.cancel()
  }

  @throws(classOf[Exception])
  @Test
  def shouldIncludeSomeOldChirpsInLiveFeed(): Unit = {
    val chirpService = server.client(classOf[ChirpService])

    val chirp1 = new Chirp("usr3", "hi 1")
    chirpService.addChirp("usr3").invoke(chirp1).toCompletableFuture().get(3, SECONDS)

    val chirp2 = new Chirp("usr4", "hi 2")
    chirpService.addChirp("usr4").invoke(chirp2).toCompletableFuture().get(3, SECONDS)

    val request = new LiveChirpsRequest(Seq("usr3", "usr4"))
    val chirps = chirpService.getLiveChirps().invoke(request).toCompletableFuture().get(3, SECONDS)
    val probe = chirps.runWith(TestSink.probe(server.system), server.materializer)
    probe.request(10)
    probe.expectNextUnordered(chirp1, chirp2);

    val chirp3 = new Chirp("usr4", "hi 3")
    chirpService.addChirp("usr4").invoke(chirp3).toCompletableFuture().get(3, SECONDS)
    probe.expectNext(chirp3)

    probe.cancel()
  }

  @throws(classOf[Exception])
  @Test
  def shouldRetrieveOldChirps(): Unit = {
    val chirpService = server.client(classOf[ChirpService])

    val chirp1 = new Chirp("usr5", "msg 1")
    chirpService.addChirp("usr5").invoke(chirp1).toCompletableFuture().get(3, SECONDS)

    val chirp2 = new Chirp("usr6", "msg 2")
    chirpService.addChirp("usr6").invoke(chirp2).toCompletableFuture().get(3, SECONDS)

    val request = new HistoricalChirpsRequest(Instant.now().minusSeconds(20), Seq("usr5", "usr6"))
    val chirps = chirpService.getHistoricalChirps().invoke(request).toCompletableFuture().get(3, SECONDS)
    val probe = chirps.runWith(TestSink.probe(server.system), server.materializer)
    probe.request(10)
    probe.expectNextUnordered(chirp1, chirp2)
    probe.expectComplete()
  }

}
