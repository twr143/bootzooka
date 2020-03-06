package issues
import issues.Problem1.Names.Ilya

/**
  * Created by Ilya Volynin on 23.12.2019 at 10:23.
  */
object Problem1 extends App {
  import Names.Name
  import cats.effect.IO
  import io.circe.Codec
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.semiauto._
  import org.http4s.EntityBody
  import sttp.tapir.{Validator, _}
  import sttp.tapir.docs.openapi._
  import sttp.tapir.json.circe._
  import sttp.tapir.openapi.circe.yaml._
  import sttp.tapir.server.ServerEndpoint

  implicit val configuration: Configuration = Configuration.default.withDiscriminator("type")

  object Names extends Enumeration {
    type Name = Value
    val Oleg = Value("OLEG")
    val Sanya = Value("SANYA")
    val Ilya = Value("Ilya")
  }

  sealed trait A
  object A {
    implicit val codec: Codec[A] = deriveConfiguredCodec
  }
  case class B(name: Name, string: String) extends A
  implicit val codec: Codec[Name] = Codec.codecForEnumeration(Names)
  implicit val validator: Validator[Name] = Validator.enum(List(Ilya)).encode(_.toString)
  implicit val schema: Schema[Name] = Schema(SchemaType.SString)
  object B {
    implicit val codec: Codec[B] = deriveConfiguredCodec
  }
  implicit val valB: Validator[B] =
    Validator.custom(b=> validator.validate(b.name).nonEmpty,"invalid name")

  //implicit val validator: Validator[String] = Validator.minLength(1) and Validator.maxLength(255)
  val endpoints: List[ServerEndpoint[_, _, _, EntityBody[IO], IO]] = List(endpoint1, endpoint2)
  def endpoint1 = endpoint.get.in(jsonBody[B].validate(valB)).out(jsonBody[A]).serverLogic[IO](_ => IO.apply[A](B(Names.Oleg, "23")).map(Right(_)))
  def endpoint2 = endpoint.post.out(jsonBody[A]).serverLogic[IO](_ => IO.apply[A](B(Names.Sanya, "23")).map(Right(_)))

  println(endpoints.toOpenAPI("", "").toYaml)
}
