package template.email.sender
import monix.eval.Task
import template.email.EmailData

trait EmailSender {
  def apply(email: EmailData): Task[Unit]
}
