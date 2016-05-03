/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl

import java.util.Collections

import scala.collection.immutable.Seq

import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

import com.lightbend.lagom.javadsl.persistence.PersistentEntity
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver

import akka.Done
import akka.actor.ActorSystem
import akka.testkit.JavaTestKit
import sample.chirper.friend.api.User


object FriendEntityTest {
  @volatile private var system: ActorSystem = _
  @BeforeClass
  def setup(): Unit = {
    system = ActorSystem.create("FriendEntityTest")
  }

  @AfterClass
  def teardown(): Unit = {
    JavaTestKit.shutdownActorSystem(system)
    system = null
  }
}

class FriendEntityTest {

  import FriendEntityTest.system
  @Test
  def testCreateUser(): Unit = {
    val driver = new PersistentEntityTestDriver(system, new FriendEntity(), "user-1")

    val outcome = driver.run(CreateUser(new User("alice", "Alice")))
    assertEquals(Done, outcome.getReplies.get(0))
    assertEquals("alice", outcome.events.get(0).asInstanceOf[UserCreated].userId)
    assertEquals("Alice", outcome.events.get(0).asInstanceOf[UserCreated].name)
    assertEquals(Collections.emptyList(), driver.getAllIssues)
  }

  @Test
  def testRejectDuplicateCreate(): Unit = {
    val driver = new PersistentEntityTestDriver(system, new FriendEntity(), "user-1")
    driver.run(new CreateUser(new User("alice", "Alice")));

    val outcome = driver.run(CreateUser(new User("alice", "Alice")))
    assertEquals(classOf[PersistentEntity.InvalidCommandException], outcome.getReplies.get(0).getClass())
    assertEquals(Collections.emptyList(), outcome.events)
    assertEquals(Collections.emptyList(), driver.getAllIssues)
  }

  @Test
  def testCreateUserWithInitialFriends(): Unit = {
    val driver = new PersistentEntityTestDriver(system, new FriendEntity(), "user-1")

    val friends = Seq("bob", "peter")
    val outcome = driver.run(CreateUser(User("alice", "Alice", friends)))
    assertEquals(Done, outcome.getReplies.get(0))
    assertEquals("alice", outcome.events.get(0).asInstanceOf[UserCreated].userId)
    assertEquals("bob", outcome.events.get(1).asInstanceOf[FriendAdded].friendId)
    assertEquals("peter", outcome.events.get(2).asInstanceOf[FriendAdded].friendId)
    assertEquals(Collections.emptyList(), driver.getAllIssues)
  }

  @Test
  def testAddFriend(): Unit = {
    val driver = new PersistentEntityTestDriver(system, new FriendEntity(), "user-1")
    driver.run(CreateUser(new User("alice", "Alice")))

    val outcome = driver.run(AddFriend("bob"), AddFriend("peter"))
    assertEquals(Done, outcome.getReplies.get(0))
    assertEquals("bob", outcome.events.get(0).asInstanceOf[FriendAdded].friendId)
    assertEquals("peter", outcome.events.get(1).asInstanceOf[FriendAdded].friendId)
    assertEquals(Collections.emptyList(), driver.getAllIssues)
  }

  @Test
  def testAddDuplicateFriend(): Unit = {
    val driver = new PersistentEntityTestDriver(system, new FriendEntity(), "user-1")
    driver.run(CreateUser(new User("alice", "Alice")))
    driver.run(AddFriend("bob"), AddFriend("peter"))

    val outcome = driver.run(AddFriend("bob"))
    assertEquals(Done, outcome.getReplies.get(0))
    assertEquals(Collections.emptyList(), outcome.events)
    assertEquals(Collections.emptyList(), driver.getAllIssues)
  }

  @Test
  def testGetUser(): Unit = {
    val driver = new PersistentEntityTestDriver(system, new FriendEntity(), "user-1")
    val alice = new User("alice", "Alice")
    driver.run(CreateUser(alice))

    val outcome = driver.run(GetUser())
    assertEquals(GetUserReply(Some(alice)), outcome.getReplies.get(0))
    assertEquals(Collections.emptyList(), outcome.events)
    assertEquals(Collections.emptyList(), driver.getAllIssues)
  }

}
