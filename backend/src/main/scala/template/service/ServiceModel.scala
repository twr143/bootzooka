package template.service
import template.infrastructure.Doobie.ConnectionIO
//import cats.implicits._
import template.infrastructure.Doobie._
//import template.util.{Id, LowerCased}
//import com.softwaremill.tagging.@@
//import template.util.LowerCased

/**
 * Created by Ilya Volynin on 18.04.2020 at 10:19.
 */class ServiceModel {
  def deleteByLogin(login: String): ConnectionIO[Int] =
    (fr"""DELETE from users where login = """ ++ Fragment.const(login)).stripMargin.update.run
//    sql"""DELETE from users where login = $login""".stripMargin.update.run

}
