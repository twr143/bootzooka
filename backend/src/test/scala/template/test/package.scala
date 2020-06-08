package template

import cats.effect.concurrent.Ref
import template.config.{Config, ConfigModule}
import com.softwaremill.quicklens._
import fs2.concurrent.SignallingRef
import monix.eval.Task

import scala.concurrent.duration._
import scala.util.Random
import monix.execution.Scheduler.Implicits.global

package object test {
  val DefaultConfig: Config = new ConfigModule {}.config
  val TestConfig: Config = DefaultConfig.modify(_.email.emailSendInterval).setTo(100.milliseconds)
  val sFlag = Ref.of[Task, Boolean](false).runSyncUnsafe()
  val alpha = "abcdefghijklmnopqrstuvwxyz"
  val size = alpha.size

  def randStr(n: Int) = (1 to n).map(x => alpha(Random.nextInt.abs % size)).mkString

}
