package template.multiflow
import monix.eval.Task
import sttp.client.SttpBackend
import template.http.Http
import template.util.BaseModule

/**
 * Created by Ilya Volynin on 10.03.2020 at 10:54.
 */
trait MultiFlowModule extends BaseModule{
  def http: Http
  lazy val mfApi = MultiFlowApi(http)
  def sttpBackend: SttpBackend[Task, Nothing, Nothing]

}
