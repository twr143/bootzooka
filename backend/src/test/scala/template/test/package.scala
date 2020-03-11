package template

import template.config.{Config, ConfigModule}
import com.softwaremill.quicklens._

import scala.concurrent.duration._
import scala.util.Random

package object test {
  val DefaultConfig: Config = new ConfigModule {}.config
  val TestConfig: Config = DefaultConfig.modify(_.email.emailSendInterval).setTo(100.milliseconds)
  val alpha = "abcdefghijklmnopqrstuvwxyz"
   val size = alpha.size

   def randStr(n:Int) = (1 to n).map(x => alpha(Random.nextInt.abs % size)).mkString

}
