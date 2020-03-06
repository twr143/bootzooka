package template.user

import doobie.util.transactor.Transactor
import monix.eval.Task
import template.email.{EmailScheduler, EmailTemplates}
import template.http.Http
import template.security.{ApiKey, ApiKeyService, Auth}
import template.util.BaseModule

trait UserModule extends BaseModule {
  lazy val userModel = new UserModel
  lazy val userApi = new UserApi(http, apiKeyAuth, userService, xa)
  lazy val userService = new UserService(userModel, emailScheduler, emailTemplates, apiKeyService, idGenerator, clock, config.user)

  def http: Http
  def apiKeyAuth: Auth[ApiKey]
  def emailScheduler: EmailScheduler
  def emailTemplates: EmailTemplates
  def apiKeyService: ApiKeyService
  def xa: Transactor[Task]
}
