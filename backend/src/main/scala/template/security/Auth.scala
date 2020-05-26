package template.security

import cats.implicits._
import java.security.SecureRandom
import java.time.{Clock, Instant}

import cats.data.{Kleisli, OptionT}
import cats.effect.Timer
import com.softwaremill.tagging._
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import template.Fail
import template.infrastructure.Doobie._
import template.user.User
import template.util._

import scala.concurrent.duration._

class Auth[T](
    authTokenOps: AuthTokenOps[T],
    xa: Transactor[Task],
    clock: Clock
) extends StrictLogging {

  // see https://hackernoon.com/hack-how-to-use-securerandom-with-kubernetes-and-docker-a375945a7b21
  private val random = SecureRandom.getInstance("SHA1PRNG")

  def checkTokenGetUserId(t: Product): Task[(Product, Id @@ User)] = {
    val id = t.productIterator.next().asInstanceOf[Id].asId[T]
    val tokenOpt = (for {
      token <- OptionT(authTokenOps.findById(id).transact(xa))
      _ <- OptionT(verifyValid(token))
    } yield token).value

    tokenOpt.>>= {
      case None =>
        logger.debug(s"Auth failed for: ${authTokenOps.tokenName} $id")
        // random sleep to prevent timing attacks
        Timer[Task].sleep(random.nextInt(1000).millis) >> Task.raiseError(Fail.UnauthorizedM(id))
      case Some(token) =>
        val delete = Task.delay(authTokenOps.deleteWhenValid).ifM(authTokenOps.delete(token).transact(xa), Task.unit)
        delete >> Task.now((t, authTokenOps.userId(token)))
    }
  }

  val checkUser: Kleisli[Task, Product, (Product, Id @@ User)] = Kleisli(checkTokenGetUserId)

  private def verifyValid(token: T): Task[Option[Unit]] = {
    if (clock.instant().isAfter(authTokenOps.validUntil(token))) {
      logger.info(s"${authTokenOps.tokenName} expired: $token")
      authTokenOps.delete(token).transact(xa).map(_ => None)
    } else {
      Task(Some(()))
    }
  }
}

/**
  * A set of operations on an authentication token, which are performed during authentication. Supports both
  * one-time tokens (when `deleteWhenValid=true`) and multi-use tokens.
  */
trait AuthTokenOps[T] {
  def tokenName: String
  def findById: (Id @@ T) => ConnectionIO[Option[T]]
  def delete: T => ConnectionIO[Unit]
  def userId: T => Id @@ User
  def validUntil: T => Instant
  def deleteWhenValid: Boolean
}
