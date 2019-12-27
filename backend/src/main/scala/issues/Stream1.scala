package issues
import cats.effect.IO

/**
  * Created by Ilya Volynin on 25.12.2019 at 15:28.
  */
object Stream1 {
  import fs2._

  def main(args: Array[String]): Unit = {
    val str = Stream(0).repeat.zipWithIndex.map(c => (c._2 * c._2).toInt).take(10)
    val l = str.toList
    val running = str.evalMap(IO(_)).compile.toList.unsafeRunSync()
    println(l)
    println(running)
  }
}
