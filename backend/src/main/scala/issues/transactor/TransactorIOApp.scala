package issues.transactor

import cats.effect._
import com.typesafe.scalalogging.StrictLogging

/**
  * Created by Ilya Volynin on 05.03.2020 at 16:38.
  */
object TransactorIOApp extends IOApp with StrictLogging with Common {
  def run(args: List[String]): IO[ExitCode] = {
    logic[IO](ExitCode.Success)
  }

}
