package template

import java.util.Locale

import cats.data.NonEmptyList
import com.softwaremill.tagging._
import monix.eval.Task
import org.http4s.EntityBody
import sttp.tapir.Validator
import sttp.tapir.server.ServerEndpoint

package object util {

  type Id <: String

  implicit class RichString(val s: String) extends AnyVal {

    def asId[T]: Id @@ T = s.asInstanceOf[Id @@ T]

    def lowerCased: String @@ LowerCased = s.toLowerCase(Locale.ENGLISH).taggedWith[LowerCased]
  }
  implicit val validator :sttp.tapir.Validator[String @@ LowerCased] = Validator.pass
  type ServerEndpoints = NonEmptyList[ServerEndpoint[_, _, _, EntityBody[Task], Task]]

  implicit class BoolUtil[F[_],A](b: Boolean) {
    def fold(f: F[A])(t: F[A]): F[A] = if (b) t else f
  }

}
