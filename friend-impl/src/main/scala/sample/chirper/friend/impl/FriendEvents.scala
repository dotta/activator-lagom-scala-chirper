/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl

import com.lightbend.lagom.javadsl.persistence.AggregateEvent
import com.lightbend.lagom.serialization.Jsonable
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag
import java.time.Instant

object FriendEvent {
  val Tag = AggregateEventTag.of(classOf[FriendEvent])
}
sealed trait FriendEvent extends AggregateEvent[FriendEvent] with Jsonable {
  override def aggregateTag(): AggregateEventTag[FriendEvent] = FriendEvent.Tag
}

case class UserCreated(userId: String, name: String, timestamp: Instant = Instant.now()) extends FriendEvent

case class FriendAdded(userId: String, friendId: String, timestamp: Instant = Instant.now()) extends FriendEvent