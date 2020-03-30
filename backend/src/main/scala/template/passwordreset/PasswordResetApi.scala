package template.passwordreset

import cats.data.{Kleisli, NonEmptyList}
import doobie.util.transactor.Transactor
import monix.eval.Task
import template.http.Http
import template.http.Http
import template.infrastructure.Json._
import template.infrastructure.Doobie._
import template.util.ServerEndpoints

class PasswordResetApi(http: Http, passwordResetService: PasswordResetService, xa: Transactor[Task]) {
  import PasswordResetApi._
  import http._

  private val PasswordResetPath = "passwordreset"

  val pResetK: Kleisli[Task, PasswordReset_IN, PasswordReset_OUT] = Kleisli { data =>
    for {
      _ <- passwordResetService.resetPassword(data.code, data.password)
    } yield PasswordReset_OUT()
  }
  private val passwordResetEndpoint = baseEndpoint.post
    .in(PasswordResetPath / "reset")
    .in(jsonBody[PasswordReset_IN])
    .out(jsonBody[PasswordReset_OUT])
    .serverLogic(pResetK mapF toOutF run)

  val forgotPK: Kleisli[Task, ForgotPassword_IN, ForgotPassword_OUT] = Kleisli { data =>
    for {
      _ <- passwordResetService.forgotPassword(data.loginOrEmail).transact(xa)
    } yield ForgotPassword_OUT()
  }

  private val forgotPasswordEndpoint = baseEndpoint.post
    .in(PasswordResetPath / "forgot")
    .in(jsonBody[ForgotPassword_IN])
    .out(jsonBody[ForgotPassword_OUT])
    .serverLogic (forgotPK mapF toOutF run)

  val endpoints: ServerEndpoints =
    NonEmptyList
      .of(
        passwordResetEndpoint,
        forgotPasswordEndpoint
      )
      .map(_.tag("passwordreset"))
}

object PasswordResetApi {
  case class PasswordReset_IN(code: String, password: String)
  case class PasswordReset_OUT()

  case class ForgotPassword_IN(loginOrEmail: String)
  case class ForgotPassword_OUT()
}
