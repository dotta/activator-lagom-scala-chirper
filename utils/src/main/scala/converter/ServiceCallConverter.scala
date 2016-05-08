/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package converter

import com.lightbend.lagom.javadsl.api.ServiceCall
import java.util.concurrent.CompletionStage
import scala.concurrent.Future
import scala.compat.java8.FutureConverters._
import java.util.concurrent.CompletableFuture
import akka.NotUsed
import scala.concurrent.ExecutionContext

object ServiceCallConverter {
  implicit def liftToServiceCall[Request, Response](f: Request => CompletionStage[Response]): ServiceCall[Request,Response] =
    new ServiceCall[Request,Response] {
      def invoke(request: Request): CompletionStage[Response] = f(request)
  }

  implicit def asCompletionStage[A](f: Future[A]): CompletionStage[A] = f.toJava
  implicit def asFuture[A](f: CompletionStage[A]): Future[A] = f.toScala
  
  implicit def asUnusedCompletionStage(f: CompletionStage[_]): CompletionStage[NotUsed] = f.thenApply(_ => NotUsed)
}