package issues.transactor
import cats.effect.{ContextShift, ExitCode, Timer}
import monix.eval.Task
import monix.eval.instances.CatsConcurrentEffectForTask
import monix.execution.Scheduler.Implicits.global

/**
 * Created by Ilya Volynin on 05.03.2020 at 17:39.
 */object TransactorMonixTask extends App with Common {
  implicit val cs: ContextShift[Task] = Task.contextShift(global)

  implicit val timer: Timer[Task] = Task.timer(global)
  implicit val effect: CatsConcurrentEffectForTask = Task.catsEffect
  logic[Task](ExitCode.Success).runSyncUnsafe()


}
