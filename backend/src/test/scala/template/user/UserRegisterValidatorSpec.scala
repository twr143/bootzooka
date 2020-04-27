package template.user

import cats.implicits._
import cats.data.Chain
import cats.data._
import cats.data.Validated._
import org.scalatest.{FlatSpec, Matchers}
import template.user.UserRegisterValidator._

class UserRegisterValidatorSpec extends FlatSpec with Matchers {
  "validate" should "accept valid data" in {
    val dataIsValid = UserRegisterValidator.validate("login", "admin@template.com", "password")

    dataIsValid shouldBe Valid("")
  }

  "validate" should "not accept login containing only empty spaces" in {
    val dataIsValid = UserRegisterValidator.validate("   ", "admin@template.com", "password")

    dataIsValid shouldBe Invalid(NonEmptySet.of(alfaNumericLogin, shortLogin))
  }

  "validate" should "not accept too short login" in {
    val tooShortLogin = "a" * (UserRegisterValidator.MinLoginLength - 1)
    val dataIsValid = UserRegisterValidator.validate(tooShortLogin, "admin@template.com", "password")

    dataIsValid shouldBe Invalid(NonEmptySet.one(shortLogin))
  }

  "validate" should "not accept too short login and should be alfanumeric" in {
    val tooShortAndAlfaNumeric = "a%"
    val dataIsValid = UserRegisterValidator.validate(tooShortAndAlfaNumeric, "admin@template.com", "password")

    dataIsValid shouldBe Invalid(NonEmptySet.of(shortLogin, alfaNumericLogin))
  }

  "validate" should "not accept too short login after trimming" in {
    val loginTooShortAfterTrim = "a" * (UserRegisterValidator.MinLoginLength - 1) + "   "
    val dataIsValid = UserRegisterValidator.validate(loginTooShortAfterTrim, "admin@template.com", "password")

    dataIsValid shouldBe Invalid(NonEmptySet.one(shortLogin))
  }

  "validate" should "not accept missing email with spaces only" in {
    val dataIsValid = UserRegisterValidator.validate("login", "   ", "password")

    dataIsValid shouldBe Invalid(NonEmptySet.one(invalidEmail))
  }

  "validate" should "not accept invalid email" in {
    val dataIsValid = UserRegisterValidator.validate("login", "invalidEmail", "password")

    dataIsValid shouldBe Invalid(NonEmptySet.one(invalidEmail))
  }

  "validate" should "not accept password with empty spaces only" in {
    val dataIsValid = UserRegisterValidator.validate("login", "admin@template.com", "    ")

    dataIsValid shouldBe Invalid(NonEmptySet.one(emptyPass))
  }
}
