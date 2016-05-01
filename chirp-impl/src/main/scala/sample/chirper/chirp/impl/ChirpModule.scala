/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package sample.chirper.chirp.impl

import com.google.inject.AbstractModule
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport

import sample.chirper.chirp.api.ChirpService

class ChirpModule extends AbstractModule with ServiceGuiceSupport {
  
  protected override def configure(): Unit =
    bindServices(serviceBinding(classOf[ChirpService], classOf[ChirpServiceImpl]))
}
