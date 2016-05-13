package converter

import java.util.concurrent.CompletionStage

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.Future

import akka.NotUsed

trait CompletionStageConverters {

  implicit def asCompletionStage[A](f: Future[A]): CompletionStage[A] = f.toJava
  implicit def asFuture[A](f: CompletionStage[A]): Future[A] = f.toScala

  implicit def asUnusedCompletionStage(f: CompletionStage[_]): CompletionStage[NotUsed] = f.thenApply(_ => NotUsed)
}

object CompletionStageConverters extends CompletionStageConverters