/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.load.api

import com.fasterxml.jackson.annotation.JsonIgnore

case class TestParams @JsonIgnore()(
  users: Int,
  friends: Int,
  chirps: Int,
  clients: Int,
  parallelism: Int,
  userIdPrefix: Option[String]
  ){

  def this() = this(1000, 10, 100000, 10, 10, Option.empty)
}
