package template.user

import cats.data.Chain
import cats.data.Validated.{Invalid, Valid}
import org.scalatest.{FlatSpec, Matchers}
import template.user.UserRegisterValidator.{EmptyPassword, InvalidEmail, ShortLogin}

class UserRegisterValidatorSpec extends FlatSpec with Matchers {
  "validate" should "accept valid data" in {
    val dataIsValid = UserRegisterValidator.validate("login", "admin@template.com", "password")

    dataIsValid shouldBe Valid(())
  }

  "validate" should "not accept login containing only empty spaces" in {
    val dataIsValid = UserRegisterValidator.validate("   ", "admin@template.com", "password")

    dataIsValid shouldBe Invalid(Chain(ShortLogin))
  }

  "validate" should "not accept too short login" in {
    val tooShortLogin = "a" * (UserRegisterValidator.MinLoginLength - 1)
    val dataIsValid   = UserRegisterValidator.validate(tooShortLogin, "admin@template.com", "password")

    dataIsValid shouldBe Invalid(Chain(ShortLogin))
  }

  "validate" should "not accept too short login after trimming" in {
    val loginTooShortAfterTrim = "a" * (UserRegisterValidator.MinLoginLength - 1) + "   "
    val dataIsValid            = UserRegisterValidator.validate(loginTooShortAfterTrim, "admin@template.com", "password")

    dataIsValid shouldBe Invalid(Chain(ShortLogin))
  }

  "validate" should "not accept missing email with spaces only" in {
    val dataIsValid = UserRegisterValidator.validate("login", "   ", "password")

    dataIsValid shouldBe Invalid(Chain(InvalidEmail))
  }

  "validate" should "not accept invalid email" in {
    val dataIsValid = UserRegisterValidator.validate("login", "invalidEmail", "password")

    dataIsValid shouldBe Invalid(Chain(InvalidEmail))
  }

  "validate" should "not accept password with empty spaces only" in {
    val dataIsValid = UserRegisterValidator.validate("login", "admin@template.com", "    ")

    dataIsValid shouldBe Invalid(Chain(EmptyPassword))
  }
}
