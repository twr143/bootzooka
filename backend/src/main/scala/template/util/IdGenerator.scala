package template.util

import java.util.UUID

import com.softwaremill.tagging._

trait IdGenerator {
  def nextId[U](): Id @@ U
}

object DefaultIdGenerator extends IdGenerator {
  override def nextId[U](): Id @@ U = UUID.randomUUID().toString.asId.taggedWith[U]
}

