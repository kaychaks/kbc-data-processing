package associatekbc

import java.nio.file.Files

import cats.Show
import cats.implicits._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Consumer, Observable}

import scala.io.Source
import scala.util.{Failure, Success}

object App extends App {
  import common.Config._

  implicit val tsvShow: Show[List[(String, String)]] = {
    case x :: xs => s"${x._1.show}\t${x._2.show}\n${xs.show}"
    case Nil => ""
  }

  implicit val tsvShow1: Show[List[String]] = {
    case x :: xs => s"${x.show}\n${xs.show}"
    case Nil => ""
  }

  val file = conf.associatekbc.inputPath

  val source = Observable.defer {
    Observable
      .fromIterator(Source.fromFile(file.value.toFile).getLines())
      .drop(1)
  }

  val consumer =
    Consumer.foldLeft[String, String](List[(String, String)]().show) {
      (xs, s) =>
        val ss = s.split(",").toList
        xs +
          List((ss.reverse.head.trim, ss.take(2).mkString("$$$").trim)).show
    }

  val lbConsumer = Consumer.loadBalance(10, consumer)

  source
    .consumeWith(lbConsumer)
    .runOnComplete {
      case Success(v) =>
        Files.write(conf.associatekbc.outputPath, v.show.getBytes)
        ()
      case Failure(e) => System.err.println(s"ERROR::\n${e.getMessage}")
    }
}
