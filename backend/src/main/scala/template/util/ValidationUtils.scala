package template.util
import cats.data.{NonEmptySet, Validated, ValidatedNec}
import cats.implicits._

/**
  * Created by Ilya Volynin on 27.04.2020 at 9:49.
  */
object ValidationUtils {
  type ValidatedNes[E, +A] = Validated[NonEmptySet[E], A]

  case class Rule(fieldName: String, mandatory: Boolean, validFunc: Any => String)

  def validateField(fieldsFieldValue: Map[String, Any], rule: Rule): ValidatedNes[String, String] = {
    val check = rule.validFunc(fieldsFieldValue(rule.fieldName))
    if (rule.mandatory)
      if (check.isEmpty)
        "".valid
      else NonEmptySet.one(check).invalid
    else "".valid
  }

  def validateFields(fields: Map[String, Any], rules: List[Rule]): ValidatedNes[String, String] =
    rules.foldLeft("".valid[NonEmptySet[String]])((set, rule) => set.combine(validateField(fields, rule)))

}
