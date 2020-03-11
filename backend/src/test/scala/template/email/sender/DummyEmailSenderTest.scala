package template.email.sender

import template.email.EmailData
import template.test.BaseTest
import monix.execution.Scheduler.Implicits.global
import template.test.BaseTest

class DummyEmailSenderTest extends BaseTest {
  it should "send scheduled email" in {
    DummyEmailSender(EmailData("test@sml.com", "subject", "content")).runSyncUnsafe()
    DummyEmailSender.findSentEmail("test@sml.com", "subject") shouldBe 'defined
  }
}
