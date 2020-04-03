package template.fileRetrieval

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.concurrent.Executors

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import io.circe.syntax._
import monix.eval.Task
import monix.execution.ExecutionModel.AlwaysAsyncExecution
import monix.execution.Scheduler
import monix.nio.file._
import monix.nio.text.UTF8Codec._
import org.http4s.EntityBody
import sttp.client._
import sttp.tapir.CodecFormat
import template.Fail
import template.http.Http
import template.infrastructure.Json._
import template.user.User

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import template.util._
import template.util.SttpUtils._
import com.softwaremill.tagging.@@
import template.security.{ApiKey, Auth}
import cats.data.{Kleisli, NonEmptyList}
import monix.nio.text.UTF8Codec.utf8Decode
import sttp.model.StatusCode
import template.util.IdUtils._
import sttp.tapir.Codec._

/**
  * Created by Ilya Volynin on 16.12.2019 at 12:12.
  */
case class FileStreamingApi(http: Http, auth: Auth[ApiKey], config: FSConfig)(implicit sttpBackend: SttpBackend[Task, Nothing, Nothing])
    extends StrictLogging {
  import FileStreamingApi._
  import http._

  private val fsPath = "fs"

  val samplesK: Kleisli[Task, Samples_IN, Samples_OUT] = Kleisli { data =>
    for {
      r <- basicRequest
        .post(uri"${config.url}")
        .body(Samples_Body_Call(data.id).asJson.toString())
        .send()
        .>>=(handleRemoteResponse[List[HutWithId]])
    } yield Samples_OUT(r)
  }
  private val samplesEndpoint = baseEndpoint.post
    .in(fsPath / "samples")
    .in(jsonBody[Samples_IN])
    .out(jsonBody[Samples_OUT])
    .serverLogic(samplesK mapF toOutF run)

  private val readFileBlocker = Executors.newFixedThreadPool(4)

  lazy val scheduler = Scheduler(readFileBlocker, AlwaysAsyncExecution)

  val fileUploadK: Kleisli[Task, (Product, Id @@ User), HutBook_OUT] = Kleisli {
    case ((_, hb: HutBook), _) =>
      for {
        from <- Task.now(java.nio.file.Paths.get(hb.file.getAbsolutePath))
        content <- readAsync(from, 30)(scheduler).pipeThrough(utf8Decode).foldL
        _ <- Task.now(Files.delete(from))
      } yield HutBook_OUT(s"$content")
  }

  private val fileUploadEndpoint = secureEndpoint.post
    .in(fsPath / "fu")
    .in(multipartBody[HutBook])
    .out(jsonBody[HutBook_OUT])
    .serverLogic(auth.checkUser >>> fileUploadK mapF toOutF run)

  val fileRetrievalK: Kleisli[Task, (Product, Id @@ User), (String, String, Array[Byte])] = Kleisli { _ =>
    for {
      r <- Task.now(("attachment; filename=resp-file.jpg", "application/octet-stream", Array(1.toByte, 2.toByte, 3.toByte)))
    } yield r
  }
  private val fileRetrievalEndpoint = secureEndpoint.get
    .in(fsPath / "fr")
    //    .in(jsonBody[HutFile_IN])
    .out(header[String]("Content-Disposition"))
    .out(header[String]("Content-Type"))
    .out(rawBinaryBody[Array[Byte]])
    .serverLogic(IdToProductK >>> auth.checkUser >>> fileRetrievalK mapF toOutF run)

  import fs2._

  val streamingK: Kleisli[Task, Unit, (String, String, String, StatusCode, Stream[Task, Byte])] = Kleisli { _ =>
    val size = 100
    for {
      r <- Stream
        .emit(List[Char]('a', 'b', 'c', 'd'))
        .repeat
        .>>=(list => Stream.chunk(Chunk.seq(list)))
        .metered[Task](100.millis)
        .take(size)
        .covary[Task]
        .map(_.toByte)
        .onFinalize(Task(logger.warn("stream finalized")))
        .pure[Task]
        .map(s => ("bytes", s"bytes 0-$size/$size", s"$size", sttp.model.StatusCode.unsafeApply(206), s))
      _ <- Task.now(logger.warn("task finished working"))
    } yield r
  }
  private val streamingEndpoint = baseEndpoint.get
    .in(fsPath / "stream")
    .out(header[String]("Accept-Ranges"))
    .out(header[String]("Content-Range"))
    .out(header[String]("Content-Length"))
    .out(statusCode)
    .out(streamBody[EntityBody[Task]](schemaFor[Byte], CodecFormat.TextPlain()))
    .serverLogic(streamingK mapF toOutF run)

  private val streamReadFileBlocker = Blocker.liftExecutionContext(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4)))

  val streamingFileK: Kleisli[Task, String, (String,  Stream[Task, Byte])] = Kleisli {
    file: String =>
          val fullPath = s"${config.fileStorage.baseDir}/$file"
          io.file
            .exists[Task](streamReadFileBlocker, Paths.get(fullPath))
            .>>=(_.fold(Task.raiseError(Fail.NotFound(s"$file")))(
                io.file
                  .readAll[Task](Paths.get(fullPath), streamReadFileBlocker, 4096)
                  .onFinalize(Task(logger.warn("file stream down finalized")))
                  .pure[Task]
                  .map(s => (s"attachment; filename=$file", s)))

            )
  }
  private val streamingFileEndpoint = baseEndpoint.get
    .in(fsPath / "streamfile")
    .in(query[String]("file"))
    .out(header[String]("Content-Disposition"))
    .out(streamBody[EntityBody[Task]](schemaFor[Byte], CodecFormat.OctetStream()))
    .serverLogic (streamingFileK mapF toOutF run)

  val endpoints: ServerEndpoints =
    NonEmptyList
      .of(
        samplesEndpoint,
        fileUploadEndpoint,
        fileRetrievalEndpoint,
        streamingEndpoint,
        streamingFileEndpoint
      )
      .map(_.tag("huts"))
}

object FileStreamingApi {

  case class Samples_IN(id: String)

  case class Samples_OUT(ids: List[HutWithId])

  case class Samples_Body_Call(id: String)

  case class HutWithId(id: String, name: String)

  case class Samples_Body_Response(huts: List[HutWithId])

  case class HutBook(title: String, file: File)

  case class HutBook_OUT(result: String)

  case class HutFile_IN(fileId: String)

}
