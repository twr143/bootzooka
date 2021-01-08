package issues

import cats.effect.concurrent.Deferred
import cats.implicits._
import fs2._
import fs2.concurrent.Topic
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._

/**
  * Created by Ilya Volynin on 30.12.2019 at 16:56.
  */
object Consumer1 {

  def main(args: Array[String]): Unit = {
    implicit class Runner[A](s: Stream[Task, A]) {
      def yolo(): Unit = s.compile.drain.runSyncUnsafe()
      def yoloV: Vector[A] = s.compile.toVector.runSyncUnsafe()
    }

    // put("hello").to[F]
    def put[A](a: A): Task[Unit] = Task.now(println(s"$a csubscribers connected"))

    def prog1(): Unit =
      Stream
        .eval {
          (Topic[Task, String]("initial"), Deferred[Task, Unit]).tupled
        }
        .>>= {
          case (topic, stop) =>
            val kafkaConsumer = Stream
              .awakeEvery[Task](1.seconds)
              .zipWithIndex
              .map(m => s"msg${m._2}")
              .repeat
              .take(3) ++ Stream.eval(stop.complete(())).drain

            val subscribers = topic.subscribers.evalTap(put)
            val subscriber1 = topic
              .subscribe(5)
              .evalTap(message => Task { println(s"subscriber1: $message") })
            val subscriber2 = topic
              .subscribe(5)
              .evalTap(message => Task { println(s"subscriber2: $message") })
              .map(_.some)
              .unNoneTerminate
              .interruptWhen(stop.get.attempt)

            val producer = kafkaConsumer.through(topic.publish)
            val consumer = Stream.sleep_[Task](200.millis) ++ topic
              .subscribe(10)
              .showLinesStdOut
              .interruptWhen(stop.get.attempt)

            consumer concurrently Stream(
              producer,
              subscribers,
              subscriber1,
              subscriber2
            ).parJoinUnbounded
        }
        .yolo()
    prog1()
  }
}
