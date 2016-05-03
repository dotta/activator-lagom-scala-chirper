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
  implicit def liftToServiceCall[Request, Response](f: Request => CompletionStage[Response]): ServiceCall[Request,Response] =
    new ServiceCall[Request,Response] {
      def invoke(request: Request): CompletionStage[Response] = f(request)
  }

  implicit def future2completionStage[A](f: Future[A]): CompletionStage[A] = f.toJava 
}