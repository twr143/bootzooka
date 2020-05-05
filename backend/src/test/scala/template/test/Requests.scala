package template.test

import java.io.File
import java.util.concurrent.Executors

import cats.effect.Blocker
import io.circe.Encoder
import template.MainModule
import template.infrastructure.Json._
import template.user.UserApi._
import monix.eval.Task
import org.http4s._
import org.http4s.syntax.all._
import template.fileRetrieval.FileStreamingApi.HutBook
import template.multiflow.MultiFlowApi.MFRequest
import org.http4s.circe._
import org.http4s.multipart.{Boundary, Multipart, Part}
import sttp.tapir.Codec

import scala.concurrent.ExecutionContext
import scala.util.Random
class Requests(val modules: MainModule) extends HttpTestSupport {

  case class RegisteredUser(login: String, email: String, password: String, apiKey: String)

  private val random = new Random()

  def randomLoginEmailPassword(): (String, String, String) =
    (randStr(12), s"user${random.nextInt(29000)}@template.com", randStr(12))

  def callMultiflowEndpoint[R <: MFRequest](r: R): Response[Task] = {
    val request = Request[Task](method = POST, uri = uri"/mf/mfep")
      .withEntity(r.asInstanceOf[MFRequest])
    modules.httpApi.mainRoutes(request).unwrap
  }

  def registerUser(login: String, email: String, password: String): Response[Task] = {
    val request = Request[Task](method = POST, uri = uri"/user/register")
      .withEntity(Register_IN(login, email, password))

    modules.httpApi.mainRoutes(request).unwrap
  }

  def newRegisteredUsed(): RegisteredUser = {
    val (login, email, password) = randomLoginEmailPassword()
    val apiKey = registerUser(login, email, password).shouldDeserializeTo[Register_OUT].apiKey
    RegisteredUser(login, email, password, apiKey)
  }

  def loginUser(loginOrEmail: String, password: String, apiKeyValidHours: Option[Int] = None): Response[Task] = {
    val request = Request[Task](method = POST, uri = uri"/user/login")
      .withEntity(Login_IN(loginOrEmail, password, apiKeyValidHours))

    modules.httpApi.mainRoutes(request).unwrap
  }

  def getUser(apiKey: String): Response[Task] = {
    val request = Request[Task](method = GET, uri = uri"/user")
    modules.httpApi.mainRoutes(authorizedRequest(apiKey, request)).unwrap
  }

  def changePassword(apiKey: String, password: String, newPassword: String): Response[Task] = {
    val request = Request[Task](method = POST, uri = uri"/user/changepassword")
      .withEntity(ChangePassword_IN(password, newPassword))

    modules.httpApi.mainRoutes(authorizedRequest(apiKey, request)).unwrap
  }

  def updateUser(apiKey: String, login: String, email: String): Response[Task] = {
    val request = Request[Task](method = POST, uri = uri"/user")
      .withEntity(UpdateUser_IN(login, email))

    modules.httpApi.mainRoutes(authorizedRequest(apiKey, request)).unwrap
  }
  private val fileBlocker = Blocker.liftExecutionContext(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4)))

  def uploadFile(apiKey: String, title: String, file: File): Response[Task] = {
    import org.http4s.EntityEncoder._
    val mp = Multipart[Task](Vector(Part.formData("title", title), Part.fileData("file", file, fileBlocker)), boundary = Boundary.create)
    val entity = multipartEncoder[Task].toEntity(mp)
    val request =
      Request[Task](method = POST,
        uri = uri"/fs/fu",
        httpVersion = HttpVersion.`HTTP/1.1`,
        headers = mp.headers,
        body = entity.body)

    modules.httpApi.mainRoutes(authorizedRequest(apiKey, request)).unwrap

  }

}
