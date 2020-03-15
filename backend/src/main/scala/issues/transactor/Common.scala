package issues.transactor
import cats.effect._
import cats.implicits._
import doobie.util.transactor.Transactor
import model.Model2.MetroLine
import template.infrastructure.Doobie._
import template.user.User

/**
  * Created by Ilya Volynin on 05.03.2020 at 17:27.
  */
trait Common {

  private def findAll: ConnectionIO[List[User]] = {
    sql"SELECT id, login, login_lowercase, email_lowercase, password, created_on FROM users"
      .query[User]
      .to[List]
  }

  def logic[F[_]: Effect: ContextShift: Timer](result: ExitCode): F[ExitCode] =
    for {
      xa <- Sync[F].delay(Transactor.fromDriverManager[F]("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/bootzooka"))
      _ <- findAll.transact(xa).map(_.map(println))
    } yield result

  private def findMetroLines: ConnectionIO[List[MetroLine]] = {
    val minStations: Option[Int] = None
    val maxStations: Option[Int] = Some(10)
    val sortDesc: Boolean = true

    val baseFr =
      fr"select id, system_id, name, station_count, track_type from metro_line"

    val minStationsFr = minStations.map(m => fr"station_count >= $m")
    val maxStationsFr = maxStations.map(m => fr"station_count <= $m")
    val whereFr = List(minStationsFr, maxStationsFr).flatten
      .reduceLeftOption(_ ++ _)
      .map(fr"where" ++ _)
      .getOrElse(fr"")

    val sortFr = fr"order by station_count" ++ (if (sortDesc) fr"desc" else fr"asc")

    (baseFr ++ whereFr ++ sortFr).query[MetroLine].to[List]
  }

  def metro[F[_]: Effect: ContextShift: Timer](result: ExitCode): F[ExitCode] =
    for {
      xa <- Effect[F].delay(
        Transactor.fromDriverManager[F]("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/jdbc-mapping?currentSchema=schema1")
      )
      _ <- findMetroLines.transact(xa).map(_.map(println))
    } yield result

}
