package template.service
import template.infrastructure.Doobie.ConnectionIO
import template.user.User
//import cats.implicits._
import template.infrastructure.Doobie._
//import template.util.{Id, LowerCased}
//import com.softwaremill.tagging.@@
//import template.util.LowerCased

/**
  * Created by Ilya Volynin on 18.04.2020 at 10:19.
  */
class ServiceModel {
  def deleteByLogin(login: String): ConnectionIO[Int] = {
    val loginStr = if (!login.contains("'")) "'" + login + "'" else login
    (fr"""DELETE from users where login = """ ++ Fragment.const(loginStr)).stripMargin.update.run
    //    sql"""DELETE from users where login = $login""".stripMargin.update.run
  }
  def query(by: String): ConnectionIO[List[User]] = {
    (sql"""SELECT id, login, login_lowercase, email_lowercase, password, created_on FROM users WHERE """ ++ Fragment.const(by))
      .query[User].to[List]
  }

}
