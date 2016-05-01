/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.chirp.api

import java.time.Instant
import java.util.UUID
import com.fasterxml.jackson.annotation.JsonIgnore

case class Chirp @JsonIgnore()(userId: String, message: String, timestamp: Instant, uuid: String) {
  def this(userId: String, message: String) =
    this(userId, message, Chirp.defaultTimestamp, Chirp.defaultUUID)
}

object Chirp {
  implicit object ChirpOrdering extends Ordering[Chirp] {
    override def compare(x: Chirp, y: Chirp): Int = x.timestamp.compareTo(y.timestamp)
  }

  def apply(userId: String, message: String, timestamp: Option[Instant], uuid: Option[String]): Chirp =
    new Chirp(userId, message, timestamp.getOrElse(defaultTimestamp), uuid.getOrElse(defaultUUID))

  private def defaultTimestamp = Instant.now()
  private def defaultUUID = UUID.randomUUID().toString()
}