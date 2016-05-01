/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.friend.impl

import com.google.inject.AbstractModule
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport
import sample.chirper.friend.api.FriendService

class FriendModule extends AbstractModule with ServiceGuiceSupport {
  override protected def configure(): Unit = {
    bindServices(serviceBinding(classOf[FriendService], classOf[FriendServiceImpl]));
  }
}
