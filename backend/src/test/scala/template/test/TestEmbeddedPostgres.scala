package template.test

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import template.config.Sensitive
import template.infrastructure.DBConfig
import com.typesafe.scalalogging.StrictLogging
import org.postgresql.PGProperty
import org.postgresql.jdbc.PgConnection
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import template.infrastructure.DBConfig

/**
  * Base trait for tests which use the database. The database is cleaned after each test.
  */
trait TestEmbeddedPostgres extends BeforeAndAfterEach with BeforeAndAfterAll with StrictLogging { self: Suite =>
  private var postgres: EmbeddedPostgres = _
  private var currentDbConfig: DBConfig = _
  var currentDb: TestDB = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    postgres = EmbeddedPostgres.builder().start()
    val url = postgres.getJdbcUrl("bz_user", "restapitest")
    postgres.getPostgresDatabase.getConnection.asInstanceOf[PgConnection].setPrepareThreshold(100)
    currentDbConfig = TestConfig.db.copy(
      username = "bz_user",
      password = Sensitive("123"),
      url = url,
      migrateOnStart = true
    )
    currentDb = new TestDB(currentDbConfig)
    currentDb.testConnection()
  }

  override protected def afterAll(): Unit = {
    postgres.close()
    currentDb.close()
    super.afterAll()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    currentDb.migrate()
  }

  override protected def afterEach(): Unit = {
    currentDb.clean()
    super.afterEach()
  }
}
