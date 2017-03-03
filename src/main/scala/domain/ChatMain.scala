package domain

import java.nio.file.{Files, StandardOpenOption}

import cats.implicits._
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Consumer, Observable}

import scala.io.Source
import scala.util.{Failure, Success}

object ChatMain extends App {

  //
  // TODO:
  // - improve the latency of the overall process by figuring out better way to parallelise Monix
  // tasks & observables
  // - once a free monad structure is introduced for domain objects, create a new interpretor
  // with Akka stream or FS2 to see if they help improve the performance
  //

  import common.Config._
  import domain.Domain.Transcript._

  val file = conf.domain.chat.inputPath
  lazy val io = Scheduler.io("custom-io", daemonic = false)

  val source = Observable.defer {
    Observable
      .fromIterator(Source.fromFile(file.value.toAbsolutePath.toFile).getLines)
      .filter(_.startsWith("""<p align="center""""))
      .map(transcript)
      .map(_.show)
  }

  val consumer =
    Consumer
      .foreachParallelAsync[String](300) { o =>
        for {
          _ <- Task.fork(
            Task.eval(
              Files.write(conf.domain.chat.outputPath,
                          s"$o".getBytes,
                          StandardOpenOption.APPEND,
                          StandardOpenOption.CREATE,
                          StandardOpenOption.WRITE)),
            io
          )
        } yield ()
      }

  source
    .consumeWith(consumer)
    .runOnComplete {
      case Success(_) =>
        io.shutdown()
        println("DONE")
      case Failure(e) =>
        io.shutdown()
        e.printStackTrace()
    }

}
