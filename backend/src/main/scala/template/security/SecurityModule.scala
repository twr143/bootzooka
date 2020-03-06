package template.security

import doobie.util.transactor.Transactor
import monix.eval.Task
import template.passwordreset.{PasswordResetAuthToken, PasswordResetCode, PasswordResetCodeModel}
import template.util.BaseModule

trait SecurityModule extends BaseModule {
  lazy val apiKeyModel = new ApiKeyModel
  lazy val apiKeyService = new ApiKeyService(apiKeyModel, idGenerator, clock)
  lazy val apiKeyAuth: Auth[ApiKey] = new Auth(new ApiKeyAuthToken(apiKeyModel), xa, clock)
  lazy val passwordResetCodeAuth: Auth[PasswordResetCode] = new Auth(new PasswordResetAuthToken(passwordResetCodeModel), xa, clock)

  def passwordResetCodeModel: PasswordResetCodeModel
  def xa: Transactor[Task]
}
