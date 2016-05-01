/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.load.impl

import java.util.concurrent.TimeUnit

case class Throughput(startTime: Long, endTime: Long, count: Long, totalCount: Long) {

  def throughput: Double = {
    if (endTime == startTime) 0.0
    else 1.0 * count * TimeUnit.SECONDS.toNanos(1) / (endTime - startTime)
  }
}
