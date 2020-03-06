package template.huts

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.concurrent.Executors

import cats.data.NonEmptyList
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

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import template.util.ServerEndpoints
import template.util.SttpUtils._

/**
  * Created by Ilya Volynin on 16.12.2019 at 12:12.
  */
case class HutsApi(http: Http, config: HutsConfig)(implicit sttpBackend: SttpBackend[Task, Nothing, Nothing]) extends StrictLogging {
  import HutsApi._
  import http._

  private val HutsPath = "huts"

  private val samplesEndpoint = baseEndpoint.post
    .in(HutsPath / "samples")
    .in(jsonBody[Samples_IN])
    .out(jsonBody[Samples_OUT])
    .serverLogic[Task] { data =>
      (for {
        r <- basicRequest
          .post(uri"${config.url}")
          .body(Samples_Body_Call(data.id).asJson.toString())
          .send()
          .flatMap(handleRemoteResponse[List[HutWithId]])
      } yield Samples_OUT(r)).toOut
    }

  private val readFileBlocker = Executors.newFixedThreadPool(4)

  lazy val scheduler = Scheduler(readFileBlocker, AlwaysAsyncExecution)

  private val fileUploadEndpoint = baseEndpoint.post
    .in(HutsPath / "fu")
    .in(multipartBody[HutBook])
    .out(jsonBody[HutBook_OUT])
    .serverLogic[Task] { hb =>
      (for {
        from <- Task.now(java.nio.file.Paths.get(hb.file.getAbsolutePath))
        content <- readAsync(from, 30)(scheduler).pipeThrough(utf8Decode).foldL
        _ <- Task.now(Files.delete(from))
      } yield HutBook_OUT(s"$content")).toOut
    }

  private val fileRetrievalEndpoint = baseEndpoint.get
    .in(HutsPath / "fr")
    //    .in(jsonBody[HutFile_IN])
    .out(header[String]("Content-Disposition"))
    .out(header[String]("Content-Type"))
    .out(binaryBody[Array[Byte]])
    .serverLogic[Task] { hf =>
      (for {
        r <- Task.now(("attachment; filename=resp-file.jpg", "application/octet-stream", Array(1.toByte, 2.toByte, 3.toByte)))
      } yield r).toOut
    }

  // endpoint.get.in("receive").out(streamBody[Source[ByteString, Any]](schemaFor[String], CodecFormat.TextPlain()))
  import fs2._

  private val streamingEndpoint = baseEndpoint.get
    .in(HutsPath / "stream")
    .out(header[String]("Accept-Ranges"))
    .out(header[String]("Content-Range"))
    .out(header[String]("Content-Length"))
    .out(statusCode)
    .out(streamBody[EntityBody[Task]](schemaFor[Byte], CodecFormat.TextPlain()))
    .serverLogic[Task] { _ =>
      val size = 100
      (for {
        r <- Stream
          .emit(List[Char]('a', 'b', 'c', 'd'))
          .repeat
          .flatMap(list => Stream.chunk(Chunk.seq(list)))
          .metered[Task](100.millis)
          .take(size)
          .covary[Task]
          .map(_.toByte)
          .onFinalize(Task(logger.warn("stream finalized")))
          .pure[Task]
          .map(s => ("bytes", s"bytes 0-$size/$size", s"$size", sttp.model.StatusCode.unsafeApply(206), s))
        _ <- Task.now(logger.warn("task finished working"))
      } yield r).toOut
    }

  private val streamReadFileBlocker = Blocker.liftExecutionContext(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4)))

  private val streamingFileEndpoint = baseEndpoint.get
    .in(HutsPath / "streamfile")
    .in(query[String]("file"))
    .out(header[String]("Content-Disposition"))
    .out(streamBody[EntityBody[Task]](schemaFor[Byte], CodecFormat.OctetStream()))
    .serverLogic[Task] { file: String =>
      val fullPath = s"${config.fileStorage.baseDir}/$file"
      io.file
        .exists[Task](streamReadFileBlocker, Paths.get(fullPath))
        .flatMap(does =>
          if (does)
            io.file
              .readAll[Task](Paths.get(fullPath), streamReadFileBlocker, 4096)
              .onFinalize(Task(logger.warn("file stream down finalized")))
              .pure[Task]
              .map(s => (s"attachment; filename=$file", s))
          else
            Task.raiseError(Fail.NotFound(s"$file"))
        )
        .toOut
    }

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

object HutsApi {

  case class Samples_IN(id: String)

  case class Samples_OUT(ids: List[HutWithId])

  case class Samples_Body_Call(id: String)

  case class HutWithId(id: String, name: String)

  case class Samples_Body_Response(huts: List[HutWithId])

  case class HutBook(title: String, file: File)

  case class HutBook_OUT(result: String)

  case class HutFile_IN(fileId: String)

}
