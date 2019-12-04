package com.softwaremill.bootzooka.util

import java.security.SecureRandom
import java.util.UUID

import com.softwaremill.tagging._
import tsec.common.SecureRandomId

trait IdGenerator {
  def nextId[U](): Id @@ U
}

object DefaultIdGenerator extends IdGenerator {
  override def nextId[U](): Id @@ U = UUID.randomUUID().toString.asId.taggedWith[U]
}
