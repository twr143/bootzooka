package template.config

case class Sensitive(value: String) extends AnyVal {
  override def toString: String = "***"
}
