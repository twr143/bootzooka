package template.email.sender

import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import sttp.client._
import template.email.{EmailData, MailgunConfig}

/**
  * Sends emails using the [[https://www.mailgun.com Mailgun]] service. The external http call is done using
  * [[sttp https://github.com/softwaremill/sttp]].
  */
class MailgunEmailSender(config: MailgunConfig)(implicit sttpBackend: SttpBackend[Task, Nothing, Nothing]) extends EmailSender with StrictLogging {
  override def apply(email: EmailData): Task[Unit] = {
    basicRequest.auth
      .basic("api", config.apiKey.value)
      .post(uri"${config.url}")
      .body(
        Map(
          "from" -> s"${config.senderDisplayName} <${config.senderName}@${config.domain}>",
          "to" -> email.recipient,
          "subject" -> email.subject,
          "html" -> email.content
        )
      )
      .send()
      .map(_ => logger.debug(s"Email to: ${email.recipient} sent"))
  }
}
