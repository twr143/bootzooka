package template.passwordreset

import java.time.Instant

import cats.implicits._
import com.softwaremill.tagging.@@
import com.typesafe.scalalogging.LazyLogging
import template.security.AuthTokenOps
import template.user.User
import template.infrastructure.Doobie._
import template.util.Id

class PasswordResetCodeModel extends LazyLogging {

  def insert(pr: PasswordResetCode): ConnectionIO[Unit] = {
    sql"""INSERT INTO password_reset_codes (id, user_id, valid_until)
         |VALUES (${pr.id}, ${pr.userId}, ${pr.validUntil})""".stripMargin.update.run.void
  }

  def delete(id: Id @@ PasswordResetCode): ConnectionIO[Unit] = {
    sql"""DELETE FROM password_reset_codes WHERE id = $id""".stripMargin.update.run.void
  }

  def findById(id: Id @@ PasswordResetCode): ConnectionIO[Option[PasswordResetCode]] = {
    sql"SELECT id, user_id, valid_until FROM password_reset_codes WHERE id = $id"
      .query[PasswordResetCode]
      .option
  }
}

case class PasswordResetCode(id: Id @@ PasswordResetCode, userId: Id @@ User, validUntil: Instant)

class PasswordResetAuthToken(passwordResetCodeModel: PasswordResetCodeModel) extends AuthTokenOps[PasswordResetCode] {
  override def tokenName: String = "PasswordResetCode"
  override def findById: Id @@ PasswordResetCode => ConnectionIO[Option[PasswordResetCode]] = passwordResetCodeModel.findById
  override def delete: PasswordResetCode => ConnectionIO[Unit] = ak => passwordResetCodeModel.delete(ak.id)
  override def userId: PasswordResetCode => Id @@ User = _.userId
  override def validUntil: PasswordResetCode => Instant = _.validUntil
  // password reset code is a one-time token
  override def deleteWhenValid: Boolean = true
}
