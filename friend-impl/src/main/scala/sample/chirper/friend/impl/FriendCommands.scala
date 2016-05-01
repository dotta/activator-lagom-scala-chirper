/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl

import com.lightbend.lagom.javadsl.persistence.PersistentEntity
import com.lightbend.lagom.serialization.Jsonable
import akka.Done
import sample.chirper.friend.api.User


sealed trait FriendCommand extends Jsonable

case class CreateUser(user: User) extends PersistentEntity.ReplyType[Done] with FriendCommand
case class GetUser() extends PersistentEntity.ReplyType[GetUserReply] with FriendCommand

case class GetUserReply(user: Option[User]) extends Jsonable
case class AddFriend(friendUserId: String) extends PersistentEntity.ReplyType[Done] with FriendCommand