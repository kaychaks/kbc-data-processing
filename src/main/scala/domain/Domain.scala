package domain

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import cats.Show
import cats.implicits._

import scala.collection.mutable
import scala.util.matching.Regex

object Domain {

  //
  // TODO:
  // - refactor regex(s) and keep them in one place
  // - introduce free monads and make the current implementation of parsing text as one of the
  // interpretor
  //

  final case class ChatOrigin(value: String) extends AnyVal
  final case class ChatText(value: String) extends AnyVal

  sealed trait Party
  final case class User(value: String) extends Party
  final case class Agent(value: String) extends Party
  final case class Blank() extends Party

  final case class Chat(from: Party, to: Party, text: String)
  final case class Conversation(cs: mutable.LinkedHashSet[Chat])

  final case class Transcript(id: UUID,
                        origDate: ZonedDateTime,
                        origin: ChatOrigin,
                        agent: Agent,
                        text: Conversation)

  object Conversation {
    def conversation: String => Agent => Conversation = { str => a =>
      val chatTextRegex = """(?=[ms]\s+\)\s*(.*?)\s*(?:\(\s+\d+[hms]|$))""".r
      val chats = for (t <- chatTextRegex.findAllMatchIn(str)) yield t.group(1)
      val chatMap: mutable.LinkedHashSet[Chat] = for {
        c <- chats.to[mutable.LinkedHashSet]
        s <- c.split(":").toList match {
          case (x :: y :: xs) if x == a.value =>
            Option(Chat(a, Blank(), (y :: xs).mkString(": ")))
          case (x :: y :: xs) if x != a.value =>
            Option(Chat(User(x), a, (y :: xs).mkString(": ")))
          case _ => None
        }
      } yield s

      val users: mutable.LinkedHashSet[Party] = for {
        Chat(x, _, _) <- chatMap
      } yield x

      Conversation {
        chatMap.collect {
          case Chat(ag, Blank(), t) =>
            Chat(ag, (users - ag).headOption.getOrElse(Blank()), t)
          case c => c
        }
      }
    }

    private def escapeString: String => String =
      _.replaceAll("\t", " ").replaceAll("\\\\", "\\\\\\\\")

    implicit val chatShow: Show[Chat] = {
      case Chat(Agent(a), _, text) =>
        s"[Agent($a)]# ${escapeString(text)}"
      case Chat(User(a), _, text) =>
        s"[User($a)]# ${escapeString(text)}"
      case _ => ""
    }

    implicit val hashSetShow: Show[mutable.LinkedHashSet[Chat]] = {
      case f: mutable.LinkedHashSet[Chat] if f.nonEmpty && f.tail.nonEmpty =>
        s"${f.head.show} ${f.tail.show}"
      case f: mutable.LinkedHashSet[Chat] if f.nonEmpty && f.tail.isEmpty =>
        s"${f.head.show}"
      case _ => ""
    }
  }

  object Transcript {
    import Conversation._

    implicit def optShow: Show[Option[Transcript]] = {
      case Some(a) => a.show
      case None => ""
    }

    implicit val transcriptShow: Show[Transcript] = (f: Transcript) =>
      s"\n${f.id.toString}\t${f.origDate.toString}\t${f.origin.value}\t${f.agent.value}\t${f.text.cs.show}"

    def transcript: String => Option[Transcript] = { s =>
      def firstMatchFirstGroup: Regex => Option[String] =
        r => for (m <- r.findFirstMatchIn(s)) yield m.group(1)

      lazy val origDate = firstMatchFirstGroup(
        """<p align="center">Chat Started: (.*?)</p>""".r)
      lazy val origin = firstMatchFirstGroup(
        """<p align="center">Chat Origin: (.*?)</p>""".r)
      lazy val agent = firstMatchFirstGroup(
        """<p align="center">Agent (.*?)</p>""".r)
      lazy val text = firstMatchFirstGroup("""</p>(\( (?:\d[hms]\s)+.*)$""".r)

      for {
        dt <- origDate
        ori <- origin
        a <- agent
        tex <- text
      } yield
        Transcript(
          java.util.UUID.randomUUID,
          ZonedDateTime.parse(dt,
                              DateTimeFormatter
                                .ofPattern("EEEE, MMMM d, y, H:m:ss (X)")),
          ChatOrigin(ori),
          Agent(a),
          conversation(tex)(Agent(a))
        )
    }
  }

}
