package template.metrics

import cats.data.Kleisli
import template.version.BuildInfo
import monix.eval.Task
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import template.http.{Error_OUT, Http}
import template.infrastructure.Json._

/**
  * Defines an endpoint which exposes the current application version information.
  */
class VersionApi(http: Http) {
  import VersionApi._
  import http._
  val versionK: Kleisli[Task, Unit, Version_OUT] = Kleisli { _ =>
    Task.now(Version_OUT(BuildInfo.builtAtString, BuildInfo.lastCommitHash))
  }
  val versionEndpoint: ServerEndpoint[Unit, (StatusCode, Error_OUT), Version_OUT, Nothing, Task] = baseEndpoint.get
    .in("version")
    .out(jsonBody[Version_OUT])
    .serverLogic (versionK mapF toOutF run)
}

object VersionApi {
  case class Version_OUT(buildDate: String, buildSha: String)
}
