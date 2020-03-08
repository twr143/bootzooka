package template.util

import java.util.UUID

import cats.data.Kleisli
import com.softwaremill.tagging._
import monix.eval.Task

trait IdGenerator {
  def nextId[U](): Id @@ U
}

object DefaultIdGenerator extends IdGenerator {
  override def nextId[U](): Id @@ U = UUID.randomUUID().toString.asId.taggedWith[U]
}

