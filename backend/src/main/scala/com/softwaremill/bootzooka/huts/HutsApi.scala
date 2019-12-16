package com.softwaremill.bootzooka.huts
import java.util.UUID
import cats.data.NonEmptyList
import com.softwaremill.bootzooka.Fail
import com.softwaremill.bootzooka.http.Http
import monix.eval.Task
import com.softwaremill.bootzooka.infrastructure.Json._
import com.softwaremill.bootzooka.util.ServerEndpoints
import com.typesafe.scalalogging.StrictLogging
import sttp.client._
import io.circe.syntax._
import com.softwaremill.bootzooka.util.SttpUtils._

/**
  * Created by Ilya Volynin on 16.12.2019 at 12:12.
  */
case class HutsApi(http: Http, config: HutsConfig)(implicit sttpBackend: SttpBackend[Task, Nothing, Nothing]) extends StrictLogging {
  import http._
  import HutsApi._

  private val HutsPath = "huts"

  private val samplesEndpoint = baseEndpoint.post
    .in(HutsPath / "samples")
    .in(jsonBody[Samples_IN])
    .out(jsonBody[Samples_OUT])
    .serverLogic[Task] { data =>
    (for {
      r <-
        basicRequest.post(uri"${config.url}")
          .body(
            Samples_Body_Call(data.id).asJson.toString()
          )
          .send()
          .flatMap(handleRemoteResponse[List[HutWithId]])
    } yield Samples_OUT(r)).toOut
  }


  val endpoints: ServerEndpoints =
    NonEmptyList
      .of(
        samplesEndpoint
      )
      .map(_.tag("huts"))
}

object HutsApi {

  case class Samples_IN(id: String)

  case class Samples_OUT(ids: List[HutWithId])

  case class Samples_Body_Call(id: String)

  case class HutWithId(id: String, name: String)

  case class Samples_Body_Response(huts: List[HutWithId])

}
