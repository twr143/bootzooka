package template

import java.nio.ByteBuffer

import cats.effect.Resource
import monix.eval.Task
import monix.reactive.Observable
import sttp.client.{SttpBackend, SttpBackendOptions}
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.monix.AsyncHttpClientMonixBackend
import template.config.ConfigModule
import template.infrastructure.DB

import scala.concurrent.duration._

/**
  * Initialised resources needed by the application to start.
  */
trait InitModule extends ConfigModule {
  lazy val db: DB = new DB(config.db)
  lazy val baseSttpBackend: Resource[Task, SttpBackend[Task, Observable[ByteBuffer], WebSocketHandler]] =
    //AsyncHttpClientMonixBackend.resource(SttpBackendOptions.Default.connectionTimeout(1 second))
    Resource.make(AsyncHttpClientMonixBackend(SttpBackendOptions.Default.connectionTimeout(1 second)))({
      _.close().map { _ =>
        logger.warn("closing async backend")
      }
    })
}
