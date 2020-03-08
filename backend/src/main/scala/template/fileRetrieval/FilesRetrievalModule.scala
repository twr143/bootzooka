package template.fileRetrieval
import monix.eval.Task
import sttp.client.SttpBackend
import template.http.Http
import template.security.{ApiKey, Auth}
import template.util.BaseModule

/**
  * Created by Ilya Volynin on 16.12.2019 at 13:36.
  */
trait FilesRetrievalModule extends BaseModule{
  def http: Http
  lazy val fsApi = FileStreamingApi(http,apiKeyAuth, config = config.fsService)(sttpBackend)
  def sttpBackend: SttpBackend[Task, Nothing, Nothing]
  def apiKeyAuth: Auth[ApiKey]

}
