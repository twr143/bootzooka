package issues

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Ilya Volynin on 06.01.2020 at 14:18.
  */
object Problem2 extends App {
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
  
  implicit val configuration: Configuration = Configuration.default.withDefaults//.withDiscriminator("type")
  
  sealed trait A

//  object A {
//
//    implicit def codec: Codec[A] = deriveConfiguredCodec
//  }

//  case class B[T](a: ArrayBuffer[T]) extends A
//
  object B {
//    implicit def codecT[T]: Codec[T] = deriveConfiguredCodec
//
//    implicit def codec[T]: Codec[B[T]] = deriveConfiguredCodec
  }

  //implicit val validator: Validator[String] = Validator.minLength(1) and Validator.maxLength(255)
//  val endpoints: List[ServerEndpoint[_, _, _, EntityBody[IO], IO]] = List(endpoint1)
//
//  def endpoint1 = endpoint.get.out(jsonBody[A]).serverLogic[IO](_ => IO(B(ArrayBuffer(1,2,3))).map(Right(_)))
//
//
//  println(endpoints.toOpenAPI("", "").toYaml)
}
