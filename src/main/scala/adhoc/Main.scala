package adhoc

import java.nio.file.Files
import java.util.UUID

import cats.Show
import cats.data.ReaderT
import cats.implicits._
import common.Config.{AppConfig, InputPath}
import monix.eval.Task
import monix.execution.Scheduler

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object Main extends App {
  sealed trait ContentType
  final case class Content(id: UUID, c: String) extends ContentType

  implicit val contentShow: Show[ContentType] = {
    case Content(id, c) => s"$id\t$c"
  }

  implicit val listContentShow: Show[List[ContentType]] = {
    case Nil     => ""
    case x :: xs => s"${x.show}\n${xs.show}"
  }

  type Error = Throwable

  def rawText: InputPath => Either[Error, String] = { in =>
    Either.fromTry {
      Try {
        Files.readAllLines(in.value).asScala.foldLeft("")((a,b) => s"$a $b")
      }
    }
  }

  def cleanse: String => Either[Error, String] = {str =>
    Right {
      str.replaceAll("[\n\t]"," ").trim
    }
  }
  def genUUID: String => Either[Error, (UUID, String)] = {str =>
    Either.fromTry {
      Try {
        (java.util.UUID.randomUUID, str)
      }
    }
  }
  def content: ((UUID, String)) => Either[Error, ContentType] =
    t => Right(Content(t._1, t._2))

  def process: InputPath => Either[Error, ContentType] =
    rawText(_) >>= cleanse >>= genUUID >>= content

  def prog: ReaderT[Task, AppConfig, Unit] = ReaderT { conf =>
    Task {
      val processed: List[ContentType] =
        conf.adhoc.inputs
          .map(process)
          .filter(_.isRight)
          .map(_.right.get)

      Files.write(conf.adhoc.outputPath, processed.show.getBytes("utf-8"))
      ()
    }
  }

  import common.Config._
  implicit lazy val ioSched = Scheduler.io("my-io", false)
  prog.run(conf).runOnComplete {
    case Success(_) => println("DONE")
    case Failure(e) => println(s"ERROR:\n${e.getMessage}")
  }
}
