/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl

import java.util.concurrent.TimeUnit.SECONDS

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration

import org.junit.Assert.assertEquals
import org.junit.Test

import com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup
import com.lightbend.lagom.javadsl.testkit.ServiceTest.eventually
import com.lightbend.lagom.javadsl.testkit.ServiceTest.withServer

import akka.NotUsed
import sample.chirper.friend.api.FriendId
import sample.chirper.friend.api.FriendService
import sample.chirper.friend.api.User

class FriendServiceTest {


  @throws(classOf[Exception])
  @Test
  def shouldBeAbleToCreateUsersAndConnectFriends() {
    withServer(defaultSetup, server => {
      val friendService = server.client(classOf[FriendService])
      val usr1 = new User("usr1", "User 1");
      friendService.createUser().invoke(usr1).toCompletableFuture().get(10, SECONDS)
      val usr2 = new User("usr2", "User 2");
      friendService.createUser().invoke(usr2).toCompletableFuture().get(3, SECONDS)
      val usr3 = new User("usr3", "User 3");
      friendService.createUser().invoke(usr3).toCompletableFuture().get(3, SECONDS)

      friendService.addFriend("usr1").invoke(FriendId(usr2.userId)).toCompletableFuture().get(3, SECONDS)
      friendService.addFriend("usr1").invoke(FriendId(usr3.userId)).toCompletableFuture().get(3, SECONDS)

      val fetchedUsr1 = friendService.getUser("usr1").invoke(NotUsed).toCompletableFuture().get(3,
          SECONDS)
      assertEquals(usr1.userId, fetchedUsr1.userId)
      assertEquals(usr1.name, fetchedUsr1.name)
      assertEquals(Seq("usr2", "usr3"), fetchedUsr1.friends)

      eventually(FiniteDuration(10, SECONDS), () => {
        val followers = friendService.getFollowers("usr2").invoke()
            .toCompletableFuture().get(3, SECONDS)
        assertEquals(Seq("usr1"), followers)
      })

    })
  }

}
