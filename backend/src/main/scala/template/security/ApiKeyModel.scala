package template.security

import java.time.Instant

import cats.implicits._
import com.softwaremill.tagging.@@
import template.infrastructure.Doobie
import template.infrastructure.Doobie._
import template.user.User
import template.util.Id


class ApiKeyModel {

  def insert(apiKey: ApiKey): ConnectionIO[Unit] = {
    sql"""INSERT INTO api_keys (id, user_id, created_on, valid_until)
         |VALUES (${apiKey.id}, ${apiKey.userId}, ${apiKey.createdOn}, ${apiKey.validUntil})""".stripMargin.update.run.void
  }

  def findById(id: Id @@ ApiKey): ConnectionIO[Option[ApiKey]] = {
    sql"""SELECT id, user_id, created_on, valid_until FROM api_keys WHERE id = $id"""
      .query[ApiKey]
      .option
  }

  def delete(id: Id @@ ApiKey): ConnectionIO[Unit] = {
    sql"""DELETE FROM api_keys WHERE id = $id""".update.run.void
  }

  def deleteByUserId(id: Id @@ User): ConnectionIO[Int] = {
    sql"""DELETE FROM api_keys WHERE user_id = $id""".update.run
  }

}

class ApiKeyAuthToken(apiKeyModel: ApiKeyModel) extends AuthTokenOps[ApiKey] {
  override def tokenName: String = "ApiKey"
  override def findById: Id @@ ApiKey => Doobie.ConnectionIO[Option[ApiKey]] = apiKeyModel.findById
  override def delete: ApiKey => Doobie.ConnectionIO[Unit] = ak => apiKeyModel.delete(ak.id)
  override def userId: ApiKey => Id @@ User = _.userId
  override def validUntil: ApiKey => Instant = _.validUntil
  override def deleteWhenValid: Boolean = false
}
