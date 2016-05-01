/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package converter

import com.lightbend.lagom.javadsl.api.ServiceCall
import java.util.concurrent.CompletionStage
import scala.concurrent.Future
import scala.compat.java8.FutureConverters.FutureOps
import java.util.concurrent.CompletableFuture

object ServiceCallConverter {
  implicit def liftToServiceCall[Id, Request, Response](f: (Id, Request) => CompletionStage[Response]): ServiceCall[Id,Request,Response] =
    new ServiceCall[Id,Request,Response] {
      def invoke(id: Id, request: Request): CompletionStage[Response] = f(id, request)
  }

  implicit def future2completionStage[A](f: Future[A]): CompletionStage[A] = f.toJava 
}