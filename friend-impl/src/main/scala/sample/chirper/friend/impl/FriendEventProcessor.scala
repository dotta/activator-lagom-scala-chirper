/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl


import java.util.UUID

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext

import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSideProcessor
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession

import akka.Done
import javax.inject.Inject

class FriendEventProcessor @Inject()(implicit ec: ExecutionContext) extends CassandraReadSideProcessor[FriendEvent] {

  // Needed to convert some Scala types to Java
  import converter.CompletionStageConverters._

  @volatile private var writeFollowers: PreparedStatement = null // initialized in prepare
  @volatile private var writeOffset: PreparedStatement = null // initialized in prepare

  private def setWriteFollowers(writeFollowers: PreparedStatement): Unit =
    this.writeFollowers = writeFollowers

  private def setWriteOffset(writeOffset: PreparedStatement): Unit =
    this.writeOffset = writeOffset

  override def aggregateTag: AggregateEventTag[FriendEvent] = FriendEvent.Tag

  override def prepare(session: CassandraSession) = {
    // @formatter:off
    prepareCreateTables(session).thenCompose(a =>
    prepareWriteFollowers(session).thenCompose(b =>
    prepareWriteOffset(session).thenCompose(c =>
    selectOffset(session))))
    // @formatter:on
  }

  private def prepareCreateTables(session: CassandraSession) = {
    // @formatter:off
    session.executeCreateTable(
        "CREATE TABLE IF NOT EXISTS follower ("
          + "userId text, followedBy text, "
          + "PRIMARY KEY (userId, followedBy))")
      .thenCompose(a => session.executeCreateTable(
        "CREATE TABLE IF NOT EXISTS friend_offset ("
          + "partition int, offset timeuuid, "
          + "PRIMARY KEY (partition))"));
    // @formatter:on
  }

  private def prepareWriteFollowers(session: CassandraSession) = {
    val statement = session.prepare("INSERT INTO follower (userId, followedBy) VALUES (?, ?)")
    statement.map(ps => {
      setWriteFollowers(ps)
      Done
    })
  }

  private def prepareWriteOffset(session: CassandraSession) = {
    val statement = session.prepare("INSERT INTO friend_offset (partition, offset) VALUES (1, ?)")
    statement.map(ps => {
      setWriteOffset(ps)
      Done
    })
  }

  private def selectOffset(session: CassandraSession) = {
    val select = session.selectOne("SELECT offset FROM friend_offset WHERE partition=1")
    select.map { maybeRow => maybeRow.map[UUID](_.getUUID("offset")) }
  }

  override def defineEventHandlers(builder: EventHandlersBuilder): EventHandlers = {
    builder.setEventHandler(classOf[FriendAdded], processFriendChanged)
    builder.build()
  }

  private def processFriendChanged(event: FriendAdded, offset: UUID) = {
    val bindWriteFollowers = writeFollowers.bind()
    bindWriteFollowers.setString("userId", event.friendId)
    bindWriteFollowers.setString("followedBy", event.userId)
    val bindWriteOffset = writeOffset.bind(offset)
    completedStatements(Seq(bindWriteFollowers, bindWriteOffset).asJava)
  }

}
