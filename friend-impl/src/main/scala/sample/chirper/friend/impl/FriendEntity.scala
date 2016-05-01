/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl

import com.lightbend.lagom.javadsl.persistence.PersistentEntity
import scala.collection.JavaConverters._
import akka.Done
import sample.chirper.friend.api.User
import java.util.Optional
import scala.compat.java8.OptionConverters._

class FriendEntity extends PersistentEntity[FriendCommand, FriendEvent, FriendState] {

  override def initialBehavior(snapshotState: Optional[FriendState]): Behavior = {
    val b = newBehaviorBuilder(snapshotState.orElseGet(() => FriendState(Option.empty)))

    b.setCommandHandler(classOf[CreateUser], (cmd: CreateUser, ctx: CommandContext[Done]) => {
      state.user match {
        case Some(_) =>
          ctx.invalidCommand(s"User ${entityId} is already created")
          ctx.done()
        case None =>
          val user = cmd.user
          val events = UserCreated(user.userId, user.name) +: user.friends.map(friendId => FriendAdded(user.userId, friendId))
          ctx.thenPersistAll(events.asJava, () => ctx.reply(Done))
      }
    })

    b.setEventHandler(classOf[UserCreated], (evt: UserCreated) => FriendState(new User(evt.userId, evt.name)))

    b.setCommandHandler(classOf[AddFriend], (cmd: AddFriend, ctx: CommandContext[Done]) => {
      state.user match {
        case None =>
          ctx.invalidCommand(s"User ${entityId} is not  created")
          ctx.done()
        case Some(user) if user.friends.contains(cmd.friendUserId) =>
          ctx.reply(Done)
          ctx.done()
        case Some(user) =>
          ctx.thenPersist(FriendAdded(user.userId, cmd.friendUserId), (evt: FriendAdded) => ctx.reply(Done))
      }
    })

    b.setEventHandler(classOf[FriendAdded], (evt: FriendAdded) => state.addFriend(evt.friendId))

    b.setReadOnlyCommandHandler(classOf[GetUser], (cmd: GetUser, ctx: ReadOnlyCommandContext[GetUserReply]) =>
      ctx.reply(GetUserReply(state.user))
    )

    b.build()
  }

}