package template.huts
import monix.eval.Task
import sttp.client.SttpBackend
import template.http.Http
import template.util.BaseModule

/**
  * Created by Ilya Volynin on 16.12.2019 at 13:36.
  */
trait HutsModule extends BaseModule{
  def http: Http
  lazy val hutsApi = HutsApi(http, config = config.hutsService)(sttpBackend)
  def sttpBackend: SttpBackend[Task, Nothing, Nothing]

}
