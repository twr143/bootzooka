package template

import cats.effect.concurrent.Ref
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import doobie.util.transactor
import fs2.concurrent.SignallingRef
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import sttp.client.SttpBackend
import template.config.Config
import template.infrastructure.CorrelationId
import template.metrics.Metrics

import scala.concurrent.duration._

object Main extends StrictLogging {
  def main(args: Array[String]): Unit = {
    CorrelationId.init()
    Metrics.init()
    Thread.setDefaultUncaughtExceptionHandler((t, e) => logger.error("Uncaught exception in thread: " + t, e))

    val initModule = new InitModule {}
    initModule.logConfig()

    val mainTask = initModule.db.transactorResource.use { _xa =>
      SignallingRef[Task, Boolean](false).flatMap { sFlag =>
        initModule.baseSttpBackend.use { _baseSttpBackend =>
          val modules = new MainModule {
            override def xa: transactor.Transactor[Task] = _xa
            override def baseSttpBackend: SttpBackend[Task, Nothing, Nothing] = _baseSttpBackend
            override def config: Config = initModule.config
            override def shutdownFlag: Ref[Task, Boolean] = sFlag
          }

          /*
        Sequencing two tasks using the >> operator:
        - the first starts the background processes (such as an email sender)
        - the second alocates the http api resource, and never releases
        it (so that the http server is available
          as long as our application runs)
           */
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
                Task.sleep(1 seconds).untilM_(sFlag.get)
              )
          ) >> Task.now(System.exit(0))
        }
      }
    }
    mainTask.runSyncUnsafe()
  }
}
