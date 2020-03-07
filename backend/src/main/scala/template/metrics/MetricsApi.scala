package template.metrics

import java.io.StringWriter

import cats.data.Kleisli
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import monix.eval.Task
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import template.http.{Error_OUT, Http}

/**
  * Defines an endpoint which exposes the current state of the metrics, which can be later read by a Prometheus server.
  */
class MetricsApi(http: Http, registry: CollectorRegistry) {
  import http._

  val metricsK: Kleisli[Task, Unit, String] = Kleisli { _ =>
    Task {
      val writer = new StringWriter
      TextFormat.write004(writer, registry.metricFamilySamples)
      writer.toString
    }
  }
  val metricsEndpoint: ServerEndpoint[Unit, (StatusCode, Error_OUT), String, Nothing, Task] = baseEndpoint.get
    .in("metrics")
    .out(stringBody)
    .serverLogic(metricsK mapF toOutF run)
}
