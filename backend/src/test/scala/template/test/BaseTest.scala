package template.test

import com.softwaremill.bootzooka.infrastructure.CorrelationId
import org.scalatest.{FlatSpec, Matchers}
import template.infrastructure.CorrelationId

trait BaseTest extends FlatSpec with Matchers {
  CorrelationId.init()
  val testClock = new TestClock()
}
