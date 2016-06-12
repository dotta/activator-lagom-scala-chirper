/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.activity.api

import sample.chirper.chirp.api.Chirp

import akka.stream.javadsl.Source

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.ScalaService._
import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.Service

trait ActivityStreamService extends Service {

  def getLiveActivityStream(userId: String): ServiceCall[NotUsed, Source[Chirp, _]]

  def getHistoricalActivityStream(userId: String): ServiceCall[NotUsed, Source[Chirp, _]]

  override def descriptor(): Descriptor = {
    // @formatter:off
    named("activityservice").withCalls(
        pathCall("/api/activity/:userId/live", getLiveActivityStream _),
        pathCall("/api/activity/:userId/history", getHistoricalActivityStream _)
      ).withAutoAcl(true)
    // @formatter:on
  }
}
