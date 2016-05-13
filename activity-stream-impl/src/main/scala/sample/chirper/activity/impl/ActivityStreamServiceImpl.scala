/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.activity.impl

import java.time.Duration
import java.time.Instant

import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext

import com.lightbend.lagom.javadsl.api.ServiceCall

import akka.NotUsed
import akka.stream.javadsl.Source
import javax.inject.Inject
import sample.chirper.activity.api.ActivityStreamService
import sample.chirper.chirp.api.Chirp
import sample.chirper.chirp.api.ChirpService
import sample.chirper.chirp.api.HistoricalChirpsRequest
import sample.chirper.chirp.api.LiveChirpsRequest
import sample.chirper.friend.api.FriendService

class ActivityStreamServiceImpl @Inject() (
    friendService: FriendService,
    chirpService: ChirpService)(implicit ec: ExecutionContext) extends ActivityStreamService {

  // Needed to convert some Scala types to Java
  import converter.ServiceCallConverter._

  override def getLiveActivityStream(userId: String): ServiceCall[NotUsed, Source[Chirp, _]] = {
    req =>
      for {
        user <- friendService.getUser(userId).invoke()
        userIds = user.friends :+ userId
        chirpsReq = LiveChirpsRequest(userIds)
        chirps <- chirpService.getLiveChirps().invoke(chirpsReq)
      } yield chirps
  }

  override def getHistoricalActivityStream(userId: String): ServiceCall[NotUsed, Source[Chirp, _]] = {
    req =>
      for {
        user <- friendService.getUser(userId).invoke()
        userIds = user.friends :+ userId
        // FIXME we should use HistoricalActivityStreamReq request parameter
        fromTime = Instant.now().minus(Duration.ofDays(7))
        chirpsReq = HistoricalChirpsRequest(fromTime, userIds)
        chirps <- chirpService.getHistoricalChirps().invoke(chirpsReq)
      } yield chirps
  }
}
