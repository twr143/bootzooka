package template.huts
import monix.eval.Task
import sttp.client.SttpBackend
import template.http.Http
import template.util.BaseModule

/**
  * Created by Ilya Volynin on 16.12.2019 at 13:36.
  */
trait FilesRetrievalModule extends BaseModule{
  def http: Http
  lazy val fsApi = FileStreamingApi(http, config = config.fsService)(sttpBackend)
  def sttpBackend: SttpBackend[Task, Nothing, Nothing]

}
