/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.api

import scala.collection.immutable.Seq
import com.fasterxml.jackson.annotation.JsonIgnore

case class User @JsonIgnore() (userId: String, name: String, friends: Seq[String]) {
  def this(userId: String, name: String) = this(userId, name, Seq.empty)
}
