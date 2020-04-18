package template.user

import java.time.Instant
import cats.implicits._
import template.util._
import cats.data.{Kleisli, NonEmptyList}
import com.softwaremill.tagging.@@
import doobie.util.transactor.Transactor
import monix.eval.Task
import template.http.Http
import template.metrics.Metrics
import template.security._
import template.util.LowerCased
import template.infrastructure.Json._
import template.infrastructure.Doobie._
import template.util.IdUtils._
import scala.concurrent.duration._

class UserApi(http: Http, auth: Auth[ApiKey], userService: UserService, xa: Transactor[Task]) {
  import UserApi._
  import http._

  private val UserPath = "user"

  val registerUserK: Kleisli[Task, Register_IN, Register_OUT] = Kleisli {
    case data: Register_IN =>
      for {
        apiKey <- userService.registerNewUser(data.login, data.email, data.password).transact(xa)
        _ <- Task(Metrics.registeredUsersCounter.inc())
      } yield Register_OUT(apiKey.id)
    }
//irate(iv_template_server_request_count{instance="localhost:8080",job="prometheus",method="post",status="2xx", classifier="/api/v1/user/register"}[10m])
  private val registerUserEndpoint = baseEndpoint.post
    .in(UserPath / "register")
    .in(jsonBody[Register_IN])
    .out(jsonBody[Register_OUT])
    .serverLogic ( registerUserK mapF toOutF run)

  val loginK: Kleisli[Task, Login_IN, Login_OUT] = Kleisli {
    case data: Login_IN =>
      for {
        apiKey <- userService
          .login(data.loginOrEmail, data.password, data.apiKeyValidHours.map(h => Duration(h.toLong, HOURS)))
          .transact(xa)
      } yield Login_OUT(apiKey.id)
  }

  private val loginEndpoint = baseEndpoint.post
    .in(UserPath / "login")
    .in(jsonBody[Login_IN])
    .out(jsonBody[Login_OUT])
    .serverLogic(loginK mapF toOutF run)

  val changePasswordK: Kleisli[Task, (Product, Id @@ User), ChangePassword_OUT] = Kleisli {
    case ((_, data: ChangePassword_IN), userId) =>
      for {
        _ <- userService.changePassword(userId, data.currentPassword, data.newPassword).transact(xa)
      } yield ChangePassword_OUT()
  }

  private val changePasswordEndpoint = secureEndpoint.post
    .in(UserPath / "changepassword")
    .in(jsonBody[ChangePassword_IN])
    .out(jsonBody[ChangePassword_OUT])
    .serverLogic(auth.checkUser >>> changePasswordK mapF toOutF run)

  val getUserK: Kleisli[Task, (Product, Id @@ User), GetUser_OUT] = Kleisli {
    case (_, userId) =>
      for {
        user <- userService.findById(userId).transact(xa)
      } yield GetUser_OUT(user.login, user.emailLowerCased, user.createdOn)
  }

  private val getUserEndpoint = secureEndpoint.get
    .in(UserPath)
    .out(jsonBody[GetUser_OUT])
    .serverLogic(IdToProductK >>> auth.checkUser >>> getUserK mapF toOutF run)

  val updateK: Kleisli[Task, (Product, Id @@ User), UpdateUser_OUT] = Kleisli {
    case ((_, data: UpdateUser_IN), userId) =>
      for {
        _ <- userService.changeUser(userId, data.login, data.email).transact(xa)
      } yield UpdateUser_OUT()
  }

  private val updateUserEndpoint = secureEndpoint.post
    .in(UserPath)
    .in(jsonBody[UpdateUser_IN])
    .out(jsonBody[UpdateUser_OUT])
    .serverLogic(auth.checkUser >>> updateK mapF toOutF run)



  val endpoints: ServerEndpoints =
    NonEmptyList
      .of(
        registerUserEndpoint,
        loginEndpoint,
        changePasswordEndpoint,
        getUserEndpoint,
        updateUserEndpoint
      )
      .map(_.tag("user"))
}

object UserApi {

  case class Register_IN(login: String, email: String, password: String)
  case class Register_OUT(apiKey: String)

  case class ChangePassword_IN(currentPassword: String, newPassword: String)
  case class ChangePassword_OUT()

  case class Login_IN(loginOrEmail: String, password: String, apiKeyValidHours: Option[Int])
  case class Login_OUT(apiKey: String)

  case class UpdateUser_IN(login: String, email: String)
  case class UpdateUser_OUT()

  case class GetUser_OUT(login: String, email: String @@ LowerCased, createdOn: Instant)

  case class DeleteUser_IN(login:String)
  case class DeleteUser_OUT(count:Int)


}
