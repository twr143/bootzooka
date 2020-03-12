package template.test

import template.config.Sensitive
import template.infrastructure.DBConfig
import com.typesafe.scalalogging.StrictLogging
import doobie.util.transactor.Transactor
import monix.eval.Task
import org.postgresql.PGProperty
import org.postgresql.jdbc.PgConnection
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import template.infrastructure.DBConfig

import scala.concurrent.Await
import scala.concurrent.duration._


/**
  * Base trait for tests which use the database. The database is cleaned after each test.
  */
trait TestEmbeddedPostgres extends BeforeAndAfterEach with BeforeAndAfterAll with StrictLogging { self: Suite =>
  private var currentDbConfig: DBConfig = _
  var currentDb: TestDB = _
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    currentDbConfig = TestConfig.db.copy(
      username = "bz_user",
      password = Sensitive("123"),
      url = "jdbc:postgresql://localhost:5432/restapitest?currentSchema=public",
      migrateOnStart = true
    )
    currentDb = new TestDB(currentDbConfig)
    currentDb.connectAndMigrate()
//        currentDb.migrate()
  }

  override protected def afterAll(): Unit = {
    Thread.sleep(1000)
    currentDb.clean()
    currentDb.close()
    super.afterAll()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
//    currentDb.migrate()
  }

  override protected def afterEach(): Unit = {
//    currentDb.clean()
    super.afterEach()
  }
}
