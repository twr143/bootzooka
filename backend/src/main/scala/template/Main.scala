package template

import cats.effect.concurrent.Deferred
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import doobie.util.transactor
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import sttp.client.SttpBackend
import template.config.Config
import template.infrastructure.CorrelationId
import template.metrics.Metrics

object Main extends StrictLogging {
  def main(args: Array[String]): Unit = {
    CorrelationId.init()
    Metrics.init()
    Thread.setDefaultUncaughtExceptionHandler((t, e) => logger.error("Uncaught exception in thread: " + t, e))

    val initModule = new InitModule {}
    initModule.logConfig()

    (initModule.db.transactorResource, initModule.baseSttpBackend)
      .mapN((_, _))
      .use {
        case (_xa, _baseSttpBackend) =>
          Deferred[Task, Unit].flatMap { signal =>
            val modules = new MainModule {
              override def xa: transactor.Transactor[Task] = _xa
              override def baseSttpBackend: SttpBackend[Task, Nothing, Nothing] = _baseSttpBackend
              override def config: Config = initModule.config
              override def shutdownSignal: Deferred[Task, Unit] = signal
            }

            modules.startBackgroundProcesses >> modules.httpApi.resource.use(_ =>
              Task
                .now("DEV".equalsIgnoreCase(initModule.config.env.mode))
                .ifM(
                  Task.delay {
                    logger.warn(s"${System.getProperty("os.name")} Press Ctrl+Z to exit...")
                    while (System.in.read() != -1) {}
                    logger.warn("Received end-of-file on stdin. Exiting")
                    // optional shutdown code here
                  },
                  signal.get
                )
            )
          }
      } runSyncUnsafe ()
  }
}
