package template.multiflow
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import sttp.client.SttpBackend
import template.http.Http
import cats.data.{Kleisli, NonEmptyList, OptionT}
import template.util.ServerEndpoints
import cats.implicits._
import io.circe.{Codec, Encoder}
import io.circe.generic.AutoDerivation
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.syntax._

/**
  * Created by Ilya Volynin on 10.03.2020 at 10:54.
  */
case class MultiFlowApi(http: Http) extends StrictLogging {
  import MultiFlowApi._
  import http._
  private val mfPath = "mf"

  val multiFlowStringK: Kleisli[OptionT[Task, *], MFRequest, MFResponse] = Kleisli {
    case MFRequestStr(s, _) => OptionT.liftF(Task.now(MFResponseString(s)))
    case _                  => OptionT.none
  }
  val multiFlowIntK: Kleisli[OptionT[Task, *], MFRequest, MFResponse] = Kleisli {
    case MFRequestInt(s, _) => OptionT.liftF(Task.now(MFResponseInt(s)))
    case _                  => OptionT.none
  }
  def optionFlat: OptionT[Task, MFResponse] => Task[MFResponse] = _.getOrElse(MFResponseDefault())
  private val multiFlowEndpoint = baseEndpoint.post
    .in(mfPath / "mfep")
    .in(jsonBody[MFRequest])
    .out(jsonBody[MFResponse])
    .serverLogic(multiFlowStringK <+> multiFlowIntK mapF optionFlat mapF toOutF run)

  val endpoints: ServerEndpoints =
    NonEmptyList
      .of(multiFlowEndpoint)
      .map(_.tag("mf"))

}
object MultiFlowApi extends AutoDerivation {
  implicit val configuration: Configuration = Configuration.default.withDiscriminator("tp")

  sealed trait MFRequest {
    def tp: String
  }
  case class MFRequestStr(s: String, tp: String = "MFRequestStr") extends MFRequest
  case class MFRequestInt(s: Int, tp: String = "MFRequestInt") extends MFRequest
  implicit val encodeMFResponse: Encoder[MFResponse] = Encoder.instance {
    case f: MFResponseString => f.asJson
    case b: MFResponseInt    => b.asJson
    case d: MFResponseDefault    => d.asJson
  }
  implicit val codecMFRequest: Codec[MFRequest] = deriveConfiguredCodec
  sealed trait MFResponse
  case class MFResponseString(s: String, desc: String = "str resp") extends MFResponse
  case class MFResponseInt(i: Int, desc: String = "int resp") extends MFResponse
  case class MFResponseDefault(desc: String = "default resp") extends MFResponse

}
