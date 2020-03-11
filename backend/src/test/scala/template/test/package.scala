package template

import template.config.{Config, ConfigModule}
import com.softwaremill.quicklens._

import scala.concurrent.duration._

package object test {
  val DefaultConfig: Config = new ConfigModule {}.config
  val TestConfig: Config = DefaultConfig.modify(_.email.emailSendInterval).setTo(100.milliseconds)
}
