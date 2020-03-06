package template.passwordreset

import doobie.util.transactor.Transactor
import monix.eval.Task
import template.email.{EmailScheduler, EmailTemplates}
import template.http.Http
import template.security.Auth
import template.user.UserModel
import template.util.BaseModule

trait PasswordResetModule extends BaseModule {
  lazy val passwordResetCodeModel = new PasswordResetCodeModel
  lazy val passwordResetService =
    new PasswordResetService(
      userModel,
      passwordResetCodeModel,
      emailScheduler,
      emailTemplates,
      passwordResetCodeAuth,
      idGenerator,
      config.passwordReset,
      clock,
      xa
    )
  lazy val passwordResetApi = new PasswordResetApi(http, passwordResetService, xa)

  def userModel: UserModel
  def http: Http
  def passwordResetCodeAuth: Auth[PasswordResetCode]
  def emailScheduler: EmailScheduler
  def emailTemplates: EmailTemplates
  def xa: Transactor[Task]
}
