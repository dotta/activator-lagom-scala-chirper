/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.chirp.api

import akka.stream.javadsl.Source

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.ScalaService._
import com.lightbend.lagom.javadsl.api.Service
import com.lightbend.lagom.javadsl.api.ServiceCall

trait ChirpService extends Service {

  def addChirp(userId: String): ServiceCall[Chirp, NotUsed]
  
  def getLiveChirps(): ServiceCall[LiveChirpsRequest, Source[Chirp, _]]
  
  def getHistoricalChirps(): ServiceCall[HistoricalChirpsRequest, Source[Chirp, _]]

  override def descriptor(): Descriptor = {
    // @formatter:off
    named("chirpservice").withCalls(
        pathCall("/api/chirps/live/:userId", addChirp _),
        namedCall("/api/chirps/live", getLiveChirps _),
        namedCall("/api/chirps/history", getHistoricalChirps _)
      ).withAutoAcl(true)
    // @formatter:on
  }
}
