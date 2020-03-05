package issues.transactor

import com.softwaremill.bootzooka.infrastructure.Doobie._
import com.softwaremill.bootzooka.user.User
import doobie.util.transactor.Transactor
import cats.effect._
import cats.implicits._

/**
 * Created by Ilya Volynin on 05.03.2020 at 17:27.
 */trait Common {
  private def findAll: ConnectionIO[List[User]] = {
    sql"SELECT id, login, login_lowercase, email_lowercase, password, created_on FROM users"
      .query[User]
      .to[List]
  }

  def logic[F[_]: Sync](result: ExitCode)(implicit cs: ContextShift[F], timer: Timer[F], e: Effect[F]): F[ExitCode] =
    for {
      xa <- Sync[F].delay(Transactor.fromDriverManager[F]("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/bootzooka"))
      _ <- findAll.transact(xa).map(_.map(println))
    } yield result

}
