/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.load.api

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.ScalaService._
import com.lightbend.lagom.javadsl.api.Service
import com.lightbend.lagom.javadsl.api.ServiceCall

import akka.stream.javadsl.Source

trait LoadTestService extends Service {

  /**
   * Example: src/test/resources/websocket-loadtest.html
   */
  def startLoad(): ServiceCall[NotUsed, Source[String, _]] 

  /**
   * Example: curl http://localhost:21360/loadHeadless -H
   * "Content-Type: application/json" -X POST -d '{"users":2000, "friends":5,
   * "chirps":200000, "clients":20, "parallelism":20}'
   */
  def startLoadHeadless(): ServiceCall[TestParams, NotUsed]

  override def descriptor(): Descriptor = {
    // @formatter:off
    named("/loadtestservice").withCalls(
        namedCall("/load", startLoad _),
        pathCall("/loadHeadless", startLoadHeadless _)
      )
    // @formatter:on
  }
}
