/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext

import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.NotFound
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession

import akka.Done
import akka.NotUsed
import converter.ServiceCallConverter._
import javax.inject.Inject
import sample.chirper.friend.api.FriendId
import sample.chirper.friend.api.FriendService
import sample.chirper.friend.api.User

class FriendServiceImpl @Inject() (
    persistentEntities: PersistentEntityRegistry,
    readSide: CassandraReadSide,
    db: CassandraSession)(implicit ec: ExecutionContext) extends FriendService {

  persistentEntities.register(classOf[FriendEntity])
  readSide.register(classOf[FriendEventProcessor])

  override def getUser(): ServiceCall[String, NotUsed, User] = {
    (id, request) =>
      friendEntityRef(id).ask[GetUserReply, GetUser](GetUser()).toScala
        .map(_.user.getOrElse(throw new NotFound(s"user $id not found")))
  }

  override def createUser(): ServiceCall[NotUsed, User, NotUsed] = {
    (id, request) =>
      friendEntityRef(request.userId).ask[Done, CreateUser](CreateUser(request)).toScala.map(_ => NotUsed)
  }

  override def addFriend(): ServiceCall[String, FriendId, NotUsed] = {
    (id, request) =>
      friendEntityRef(id).ask[Done, AddFriend](AddFriend(request.friendId)).toScala.map(_ => NotUsed)
  }

  override def getFollowers(): ServiceCall[String, NotUsed, Seq[String]] = {
    (userId, req) =>
      {
        db.selectAll("SELECT * FROM follower WHERE userId = ?", userId).toScala.map { jrows =>
          val rows = jrows.asScala.toVector
          rows.map(_.getString("followedBy"))
        }
      }
  }

  private def friendEntityRef(userId: String) =
    persistentEntities.refFor(classOf[FriendEntity], userId)
}