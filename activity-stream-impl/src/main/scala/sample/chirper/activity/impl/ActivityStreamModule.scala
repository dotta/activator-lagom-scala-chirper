/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.activity.impl

import com.google.inject.AbstractModule
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport

import sample.chirper.activity.api.ActivityStreamService
import sample.chirper.chirp.api.ChirpService
import sample.chirper.friend.api.FriendService

class ActivityStreamModule extends AbstractModule with ServiceGuiceSupport {

  override def configure(): Unit = {
    bindServices(serviceBinding(classOf[ActivityStreamService], classOf[ActivityStreamServiceImpl]))
    bindClient(classOf[FriendService])
    bindClient(classOf[ChirpService])
  }
}
