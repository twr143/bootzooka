package issues
import cats.implicits._
import monix.eval.Task

import scala.concurrent.duration._
import monix.execution.Scheduler.Implicits.global
import monix.eval.instances.CatsConcurrentForTask

import scala.concurrent.Await

/**
  * Created by Ilya Volynin on 25.12.2019 at 15:28.
  */
object Stream1 {
  import fs2._

  def main(args: Array[String]): Unit = {
    //    val str = Stream(0).repeat.zipWithIndex.map(c => (c._2 * c._2).toInt).take(10)
    //    val l = str.toList
    //    val running = str.evalMap(IO(_)).compile.toList.unsafeRunSync()
    //    println(l)
    //    println(running)
        val l2 = Stream.range(1, 10)
      .metered[Task](1 second)
      .covary[Task]
      .onFinalize(Task.now(println("finalized"))).compile.toList.runSyncUnsafe()

//      .andThen{case scala.util.Success(value) => println(s"list: $value")}//(either => either.fold(tw => println(tw.getMessage),lst => println(s"list: $lst")))
//    scala.concurrent.Await.result(l2, 13.seconds)

    //    Stream(1,2,3,4).broadcast[Task].map { worker =>
    //             worker.evalMap { o => Task.now(println("1:" + o.toString)) }
    //           }.take(3).parJoinUnbounded.compile.drain.runSyncUnsafe()
//    val r = Stream(1, 2, 3, 4).balance[Task](1).map { worker =>
//      worker.map { i => println(s"thread id: ${Thread.currentThread().getId}, i =$i"); i.toString }
//    }.take(1)//number of threads
//      .parJoinUnbounded.compile.to[Set].runSyncUnsafe()
//    println(r)
  }
}
