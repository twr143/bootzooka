package template.util
import cats.data.Kleisli
import monix.eval.Task

/**
 * Created by Ilya Volynin on 08.03.2020 at 14:16.
*/
object IdUtils{
    def IdToProductK: Kleisli[Task, Id, Product] = Kleisli(id => Task.now(new Tuple1[Id](id)))
}
