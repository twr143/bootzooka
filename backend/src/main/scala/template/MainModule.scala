package template

import java.time.Clock

import cats.data.NonEmptyList
import monix.eval.Task
import template.email.EmailModule
import template.http.{Http, HttpApi}
import template.fileRetrieval.FilesRetrievalModule
import template.infrastructure.InfrastructureModule
import template.metrics.MetricsModule
import template.multiflow.MultiFlowModule
import template.passwordreset.PasswordResetModule
import template.security.SecurityModule
import template.user.UserModule
import template.util.{DefaultIdGenerator, IdGenerator, ServerEndpoints}

/**
  * Main application module. Depends on resources initalised in [[InitModule]].
  */
trait MainModule
    extends SecurityModule
    with EmailModule
    with UserModule
    with PasswordResetModule
    with MetricsModule
    with InfrastructureModule
    with FilesRetrievalModule
    with MultiFlowModule {

  override lazy val idGenerator: IdGenerator = DefaultIdGenerator
  override lazy val clock: Clock = Clock.systemUTC()

  lazy val http: Http = new Http()

  private lazy val endpoints: ServerEndpoints = userApi.endpoints concatNel passwordResetApi.endpoints concatNel fsApi.endpoints concatNel mfApi.endpoints
  private lazy val adminEndpoints: ServerEndpoints = NonEmptyList.of(metricsApi.metricsEndpoint, versionApi.versionEndpoint)

  lazy val httpApi: HttpApi = new HttpApi(http, endpoints, adminEndpoints, collectorRegistry, config.api)

  lazy val startBackgroundProcesses: Task[Unit] = emailService.startProcesses().void
}
