package template.multiflow
import java.sql.Connection
import java.time.Clock

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, Resource}
import doobie.free.KleisliInterpreter
import doobie.util.transactor.Strategy
import monix.eval.Task
import org.flywaydb.core.internal.database.postgresql.PostgreSQLConnection
import org.scalatest.concurrent.Eventually
import sttp.client.{NothingT, SttpBackend}
import sttp.client.impl.monix.TaskMonadAsyncError
import sttp.client.testing.SttpBackendStub
import template.MainModule
import template.config.Config
import template.infrastructure.Doobie.Transactor
import template.multiflow.MultiFlowApi._
import template.test._

import scala.concurrent.ExecutionContext

/**
  * Created by Ilya Volynin on 12.03.2020 at 16:03.
  */
class MultiFlowTest extends BaseTest with Eventually {
  lazy val modules: MainModule = new MainModule {
    override def xa: Transactor[Task] = Transactor(
      (),
      (_: Unit) => Resource.pure[Task, Connection](null),
      KleisliInterpreter[Task](Blocker.liftExecutionContext(ExecutionContext.global)).ConnectionInterpreter,
      Strategy.void
    )
    override lazy val baseSttpBackend: SttpBackend[Task, Nothing, NothingT] = SttpBackendStub(TaskMonadAsyncError)
    override lazy val config: Config = TestConfig
    override lazy val clock: Clock = testClock
    def shutdownFlag: Ref[Task, Boolean] = sFlag
  }

  val requests = new Requests(modules)
  import requests._
  "/mf/mfep" should "respond MFResponseString" in {
    val str = "s123"
    val response1 = callMultiflowEndpoint(MFRequestStr(str))
    response1.status shouldBe Ok
    response1.shouldDeserializeTo[MFResponseString].s shouldBe str
  }
  "/mf/mfep" should "respond MFResponseInt" in {
    val i = 123
    val response1 = callMultiflowEndpoint(MFRequestInt(i))
    response1.status shouldBe Ok
    response1.shouldDeserializeTo[MFResponseInt].i shouldBe i
  }

}
