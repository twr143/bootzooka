package template.service
import cats.effect.concurrent.Deferred
import doobie.util.transactor.Transactor
import monix.eval.Task
import template.email.{EmailScheduler, EmailTemplates}
import template.http.Http
import template.security.{ApiKey, ApiKeyService, Auth}
import template.util.BaseModule

/**
  * Created by Ilya Volynin on 18.04.2020 at 9:57.
  */
trait ServiceModule extends BaseModule {

  lazy val serviceModel = new ServiceModel
  lazy val serviceApi = new ServiceApi(http, serviceService, xa, shutdownSignal)
  lazy val serviceService = new ServiceService(serviceModel)

  def http: Http
  def apiKeyAuth: Auth[ApiKey]
  def emailScheduler: EmailScheduler
  def emailTemplates: EmailTemplates
  def apiKeyService: ApiKeyService
  def xa: Transactor[Task]
  def shutdownSignal: Deferred[Task, Unit]
}
