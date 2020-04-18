package template.service
import cats.implicits._
import template.util._
import cats.data.{Kleisli, NonEmptyList}
import cats.data.NonEmptyList
import doobie.util.transactor.Transactor
import monix.eval.Task
import template.http.Http
import template.user.UserService
import template.util.ServerEndpoints
import monix.eval.Task
import template.http.Http
import template.metrics.Metrics
import template.security._
import template.util.LowerCased
import template.infrastructure.Json._
import template.infrastructure.Doobie._
import template.util.IdUtils._
import scala.concurrent.duration._



/**
 * Created by Ilya Volynin on 18.04.2020 at 9:58.
 */
class ServiceApi(http: Http, serviceService: ServiceService, xa: Transactor[Task]) {
import ServiceApi._
  import http._
  private val UserPath = "user"

  val deleteUserK: Kleisli[Task, DeleteUser_IN, DeleteUser_OUT] = Kleisli {
    dui =>
      for {
        result <- serviceService.deleteUser(dui.login).transact(xa)
      } yield DeleteUser_OUT(count = result)
  }
//irate(iv_template_server_request_count{instance="localhost:8080",job="prometheus",method="post",status="2xx", classifier="/api/v1/user/delete"}[5m])
  private val deleteUserEndpoint = baseEndpoint.post
    .in(UserPath / "delete")
    .in(jsonBody[DeleteUser_IN])
    .out(jsonBody[DeleteUser_OUT])
    .serverLogic(deleteUserK mapF toOutF run)

  val endpoints: ServerEndpoints =
    NonEmptyList
      .of(
        deleteUserEndpoint
      )

}
object ServiceApi{
  case class DeleteUser_IN(login:String)
  case class DeleteUser_OUT(count:Int)

}
