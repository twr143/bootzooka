package template.util
import cats.data.{NonEmptySet, Validated, ValidatedNec}
import cats.implicits._

/**
  * Created by Ilya Volynin on 27.04.2020 at 9:49.
  */
object ValidationUtils {
  case class Rule(fieldName: String, mandatory: Boolean, validFunc: Any => String)

  def validateField(fieldsFieldValue: Map[String, Any], rule: Rule): Validated[NonEmptySet[String], String] = {
    val check = rule.validFunc(fieldsFieldValue(rule.fieldName))
    if (rule.mandatory)
      if (check.isEmpty)
        "".valid
      else NonEmptySet.one(check).invalid
    else "".valid
  }

  def validateFields(fields: Map[String, Any], rules: List[Rule]): Validated[NonEmptySet[String], String] =
    rules.foldLeft("".valid[NonEmptySet[String]])((chain, rule) => chain.combine(validateField(fields, rule)))

}
