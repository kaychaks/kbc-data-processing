package domain

import cats.Show
import java.nio.file.Files

import common.Config.AppConfig
import java.util.UUID
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{Consumer, Observable}
import cats.implicits._
import scala.util.{Failure, Success}

object FAQMain extends App {

  sealed trait FAQType
  final case class FAQ(id: UUID, q: String, a: String) extends FAQType

  implicit val faqShow: Show[FAQType] = {
    case FAQ(id, q, a) => s"$id\t$q\t$a"
  }

  implicit val faqsShow: Show[List[FAQType]] = {
    case Nil => ""
    case x :: xs => s"${x.show}\n${xs.show}"
  }

  def parse: String => List[FAQType] = { line =>
    val qaReg =
      """(?=question":"(.*?)","answer":"\[(.*?)\]".*?(?=question|$))""".r

    def cleanse: String => String = _.trim.replaceAll("\t", " ")

    val faqs = for {
      qa <- qaReg.findAllMatchIn(line).map(m => (m.group(1), m.group(2)))
//      (q, a) = qa
    } yield
      FAQ(
        java.util.UUID.randomUUID,
        cleanse(qa._1),
        cleanse(qa._2)
      )

    faqs.toList
  }

  def prog: AppConfig => Unit = { conf =>
    implicit lazy val ioSched = Scheduler.io("my-io", false)

    val in = Files.newBufferedReader(conf.domain.faq.inputPath.value)
    val ob = Observable.fromLinesReader(in)
    val defOb = Observable.defer(ob)

    val source: Observable[FAQType] =
      defOb
        .mapTask { line =>
          Task {
            parse(line)
          }
        }
        .foldLeftF[List[FAQType]](List[FAQType]()) { (acc, ls) =>
          acc ++ ls
        }
        .flatMap(Observable.fromIterable)

    val sink: Consumer[FAQType, List[FAQType]] =
      Consumer.foldLeftAsync[List[FAQType], FAQType](List[FAQType]()) {
        (acc, f) =>
          Task {
            f :: acc
          }
      }

    source.consumeWith(sink).runOnComplete {
      case Success(fs) => {
        Files.write(conf.domain.faq.outputPath, fs.show.getBytes("utf-8"))
        ioSched.shutdown()
        println("Done")
      }
      case Failure(e) => ioSched.shutdown(); e.printStackTrace
    }
    ()
  }

  import common.Config._
  prog(conf)
}
