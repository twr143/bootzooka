package issues.transactor
import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.scalalogging.StrictLogging

/**
 * Created by Ilya Volynin on 06.03.2020 at 11:45.
 */object MetroIOApp   extends IOApp with StrictLogging with Common {
    def run(args: List[String]): IO[ExitCode] = {
      metro[IO](ExitCode.Success)
    }
}
