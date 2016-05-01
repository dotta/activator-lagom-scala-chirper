/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.activity.api

import java.time.Instant

case class HistoricalActivityStreamReq(fromTime: Instant)
