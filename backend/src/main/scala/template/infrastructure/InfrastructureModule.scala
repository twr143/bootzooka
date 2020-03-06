package template.infrastructure

import monix.eval.Task
import sttp.client.prometheus.PrometheusBackend
import sttp.client.{NothingT, SttpBackend}

trait InfrastructureModule {
  lazy val sttpBackend: SttpBackend[Task, Nothing, NothingT] = new SetCorrelationIdBackend(
    new LoggingSttpBackend[Task, Nothing, NothingT](PrometheusBackend[Task, Nothing](baseSttpBackend))
  )

  def baseSttpBackend: SttpBackend[Task, Nothing, NothingT]
}
