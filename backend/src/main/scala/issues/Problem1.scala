package issues

/**
  * Created by Ilya Volynin on 23.12.2019 at 10:23.
  */
object Problem1 extends App {
  import Names.Name
  import cats.effect.IO
  import org.http4s.EntityBody
  import io.circe.Codec
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.semiauto._
  import sttp.tapir.server.ServerEndpoint
  import sttp.tapir.Validator
  import sttp.tapir.json.circe._
  import sttp.tapir._
  import sttp.tapir.docs.openapi._
  import sttp.tapir.openapi.circe.yaml._

  implicit val configuration: Configuration = Configuration.default.withDiscriminator("type")

  object Names extends Enumeration {
    type Name = Value
    val Oleg = Value("OLEG")
    val Sanya = Value("SANYA")
    implicit val codec: Codec[Name] = Codec.codecForEnumeration(this)
    implicit val validator: Validator[Name] = Validator.enum(List(Oleg, Sanya)).encode(_.toString)
    implicit val schema: Schema[Name] = Schema(SchemaType.SString)
  }

  sealed trait A
  object A {
    implicit val codec: Codec[A] = deriveConfiguredCodec
  }
  case class B(name: Name, string: String) extends A
  object B {
    implicit val codec: Codec[B] = deriveConfiguredCodec
  }

  //implicit val validator: Validator[String] = Validator.minLength(1) and Validator.maxLength(255)
  val endpoints: List[ServerEndpoint[_, _, _, EntityBody[IO], IO]] = List(endpoint1, endpoint2)
  def endpoint1 = endpoint.get.out(jsonBody[A]).serverLogic[IO](_ => IO.apply[A](B(Names.Oleg, "23")).map(Right(_)))
  def endpoint2 = endpoint.post.out(jsonBody[A]).serverLogic[IO](_ => IO.apply[A](B(Names.Oleg, "23")).map(Right(_)))

  println(endpoints.toOpenAPI("", "").toYaml)
}
