package template.security

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}

import com.softwaremill.tagging.@@
import com.typesafe.scalalogging.StrictLogging
import template.infrastructure.Doobie._
import template.user.User
import template.util.{Id, IdGenerator}

import scala.concurrent.duration.Duration

class ApiKeyService(apiKeyModel: ApiKeyModel, idGenerator: IdGenerator, clock: Clock) extends StrictLogging {

  def create(userId: Id @@ User, valid: Duration): ConnectionIO[ApiKey] = {
    val now = clock.instant()
    val validUntil = now.plus(valid.toMinutes, ChronoUnit.MINUTES)
    val apiKey = ApiKey(idGenerator.nextId[ApiKey](), userId, now, validUntil)

    logger.debug(s"Creating a new api key for user $userId, valid until: $validUntil")
    apiKeyModel.insert(apiKey).map(_ => apiKey)
  }
}

case class ApiKey(id: Id @@ ApiKey, userId: Id @@ User, createdOn: Instant, validUntil: Instant)
