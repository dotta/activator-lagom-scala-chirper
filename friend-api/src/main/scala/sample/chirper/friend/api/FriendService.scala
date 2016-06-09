/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.api

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.ScalaService._
import com.lightbend.lagom.javadsl.api.Service
import com.lightbend.lagom.javadsl.api.ServiceCall
import scala.collection.immutable.Seq


/**
 * The friend service.
 */
trait FriendService extends Service {

  /**
   * Service call for getting a user.
   *
   * The ID of this service call is the user name, and the response message is the User object.
   */
  def getUser(id: String): ServiceCall[NotUsed, User] 

  /**
   * Service call for creating a user.
   *
   * The request message is the User to create.
   */
  def createUser(): ServiceCall[User, NotUsed]

  /**
   * Service call for adding a friend to a user.
   *
   * The ID for this service call is the ID of the user that the friend is being added to.
   * The request message is the ID of the friend being added.
   */
  def addFriend(userId: String): ServiceCall[FriendId, NotUsed]

  /**
   * Service call for getting the followers of a user.
   *
   * The ID for this service call is the Id of the user to get the followers for.
   * The response message is the list of follower IDs.
   */
  def getFollowers(id: String): ServiceCall[NotUsed, Seq[String]]

  override def descriptor(): Descriptor = {
    // @formatter:off
    named("friendservice").withCalls(
        pathCall("/api/users/:id", getUser _),
        namedCall("/api/users", createUser _),
        pathCall("/api/users/:userId/friends", addFriend _),
        pathCall("/api/users/:id/followers", getFollowers _)
      ).withAutoAcl(true)
    // @formatter:on
  }
}
