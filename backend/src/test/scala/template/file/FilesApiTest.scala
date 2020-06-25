package template.file
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.time.Clock

import cats.effect.concurrent.Ref
import monix.eval.Task
import org.http4s.Status
import org.scalatest.concurrent.Eventually
import sttp.client.{NothingT, SttpBackend}
import sttp.client.impl.monix.TaskMonadAsyncError
import sttp.client.testing.SttpBackendStub
import template.MainModule
import template.config.Config
import template.fileRetrieval.FileStreamingApi.HutBook_OUT
import template.infrastructure.Doobie.Transactor
import template.test._
import template.user.UserApi.Register_OUT
import template.infrastructure.Json._

/**
  * Created by Ilya Volynin on 04.05.2020 at 19:10.
  */
class FilesApiTest extends BaseTest with TestEmbeddedPostgres with Eventually {
  lazy val modules: MainModule = new MainModule {
    override def xa: Transactor[Task] = currentDb.xa
    override lazy val baseSttpBackend: SttpBackend[Task, Nothing, NothingT] = SttpBackendStub(TaskMonadAsyncError)
    override lazy val config: Config = TestConfig
    override lazy val clock: Clock = testClock
    def shutdownSignal = sFlag
  }

  val requests = new Requests(modules)
  import requests._
  "/fs/fu" should "respond contents of uploaded file" in {
    val (login, email, password) = randomLoginEmailPassword()

    // when
    val response1 = registerUser(login, email, password)

    // then
    response1.status shouldBe Status.Ok
    val apiKey = response1.shouldDeserializeTo[Register_OUT].apiKey
    val title = "Some file caption"
    val contentsB = Array(1.toByte, 2.toByte, 3.toByte)
    import java.io.File
    import java.io.FileOutputStream
    val tempFile = File.createTempFile("tmp", ".tmp")
    val fos = new FileOutputStream(tempFile)
    fos.write(contentsB)
    val uploadResponse = uploadFile(apiKey, title, tempFile)
    uploadResponse.status shouldBe Status.Ok
    val contentsS = uploadResponse.shouldDeserializeTo[HutBook_OUT].result
    val str = new String(contentsB, StandardCharsets.UTF_8)
    contentsS shouldBe str

  }

}
