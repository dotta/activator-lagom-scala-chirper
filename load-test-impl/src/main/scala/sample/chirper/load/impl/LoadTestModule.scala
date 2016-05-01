/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.load.impl

import com.google.inject.AbstractModule
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport

import sample.chirper.activity.api.ActivityStreamService
import sample.chirper.chirp.api.ChirpService
import sample.chirper.friend.api.FriendService
import sample.chirper.load.api.LoadTestService

class LoadTestModule extends AbstractModule with ServiceGuiceSupport {
  override protected def configure(): Unit = {
    bindServices(serviceBinding(classOf[LoadTestService], classOf[LoadTestServiceImpl]))
    bindClient(classOf[FriendService])
    bindClient(classOf[ChirpService])
    bindClient(classOf[ActivityStreamService])
  }
}
