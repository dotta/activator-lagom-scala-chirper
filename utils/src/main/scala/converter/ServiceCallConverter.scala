/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package converter

import java.util.concurrent.CompletionStage

import akka.japi.Pair
import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.{RequestHeader, ResponseHeader}
import com.lightbend.lagom.javadsl.server.HeaderServiceCall

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

object ServiceCallConverter extends CompletionStageConverters {
  implicit def liftToServiceCall[Request, Response](f: Request => CompletionStage[Response]): ServiceCall[Request, Response] =
    new ServiceCall[Request, Response] {
      def invoke(request: Request): CompletionStage[Response] = f(request)
    }

  implicit def liftToHeaderServiceCall[Request, Response](f: (RequestHeader, Request) => CompletionStage[Response])
    (implicit ec: ExecutionContext): HeaderServiceCall[Request, Response] =
      new HeaderServiceCall[Request, Response] {
        override def invokeWithHeaders(requestHeader: RequestHeader, request: Request): CompletionStage[Pair[ResponseHeader, Response]] =
          f(requestHeader, request).map(response => Pair.create(ResponseHeader.OK, response))
      }
}
