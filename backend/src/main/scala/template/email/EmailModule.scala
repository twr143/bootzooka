package template.email

import doobie.util.transactor.Transactor
import monix.eval.Task
import template.email.sender.{DummyEmailSender, EmailSender, SmtpEmailSender}
import template.util.BaseModule

trait EmailModule extends BaseModule {
  lazy val emailModel = new EmailModel
  lazy val emailService = new EmailService(emailModel, idGenerator, emailSender, config.email, xa)
  // the EmailService implements the EmailScheduler functionality - hence, creating an alias for this dependency
  lazy val emailScheduler: EmailScheduler = emailService
  lazy val emailTemplates = new EmailTemplates()
  // depending on the configuration, creating the appropriate EmailSender instance
  lazy val emailSender: EmailSender = /*if (config.email.mailgun.enabled) {
    new MailgunEmailSender(config.email.mailgun)(sttpBackend)
  } else*/ if (config.email.smtp.enabled) {
    new SmtpEmailSender(config.email.smtp)
  } else {
    DummyEmailSender
  }

  def xa: Transactor[Task]
  //def sttpBackend: SttpBackend[Task, Nothing, Nothing]
}
