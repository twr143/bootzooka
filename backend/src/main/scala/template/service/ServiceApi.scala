package template.service
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import template.util._
import cats.data.{Kleisli, NonEmptyList}
import cats.data.NonEmptyList
import doobie.util.transactor.Transactor
import io.circe.{Codec, Encoder}
import io.circe.generic.AutoDerivation
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import monix.eval.Task
import template.http.Http
import template.util.ServerEndpoints
import monix.eval.Task
import template.http.Http
import template.infrastructure.Doobie._

import io.circe.generic.extras.semiauto._

/**
  * Created by Ilya Volynin on 18.04.2020 at 9:58.
  */
class ServiceApi(http: Http, serviceService: ServiceService, xa: Transactor[Task]) {
  import ServiceApi._
  import http._
  private val UserPath = "user"

  val deleteUserK: Kleisli[Task, DeleteUser_IN, DeleteUser_OUT] = Kleisli { dui =>
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

  val queryUsersK: Kleisli[Task, QueryUsers_IN, QueryUsers_OUT] = Kleisli { qui =>
    for {
      uResult <- serviceService.queryUsers(qui.by).transact(xa)
    } yield QueryUsers_OUT(users = uResult.map(u => UserOut(u.login, u.emailLowerCased, u.createdOn, u.id)))
  }

  private val queryUsersEndpoint = baseEndpoint.post
    .in(UserPath / "query")
    .in(jsonBody[QueryUsers_IN])
    .out(jsonBody[QueryUsers_OUT])
    .serverLogic(queryUsersK mapF toOutF run)

  val endpoints: ServerEndpoints =
    NonEmptyList
      .of(
        deleteUserEndpoint,
        queryUsersEndpoint
      )

}
object ServiceApi extends AutoDerivation {
  case class UserOut(login: String, emailLowerCased: String, createdOn: Instant, id: String)
  implicit val configuration: Configuration = Configuration.default.withDiscriminator("tp")
//  implicit val codecInstant: Codec[Instant] = deriveConfiguredCodec
  val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
  implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](i=>formatter.format(LocalDateTime.ofInstant(i, ZoneOffset.ofHours(3))))

  implicit val codecUser: Codec[UserOut] = deriveConfiguredCodec

  case class DeleteUser_IN(login: String)
  case class DeleteUser_OUT(count: Int)

  case class QueryUsers_IN(by: String)
  case class QueryUsers_OUT(users: List[UserOut])

}
