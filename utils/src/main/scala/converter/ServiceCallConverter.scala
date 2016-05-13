/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package converter

import java.util.concurrent.CompletionStage

import com.lightbend.lagom.javadsl.api.ServiceCall

object ServiceCallConverter extends CompletionStageConverters {
  implicit def liftToServiceCall[Request, Response](f: Request => CompletionStage[Response]): ServiceCall[Request,Response] =
    new ServiceCall[Request,Response] {
      def invoke(request: Request): CompletionStage[Response] = f(request)
  }
}