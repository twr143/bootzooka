package issues
import scala.util.Random

/**
  * Created by Ilya Volynin on 06.01.2020 at 14:18.
  */
object Problem2 extends App {
  private val random = new Random()
  val alpha = "abcdefghijklmnopqrstuvwxyz"
  val size = alpha.size

  def randStr(n:Int) = (1 to n).map(x => alpha(Random.nextInt.abs % size)).mkString

    def randomLoginEmailPassword(): (String, String, String) =
      (randStr(12), s"user${random.nextInt(9000)}@bootzooka.com", randStr(12).mkString)

    println(randomLoginEmailPassword().toString())
}
