package com.softwaremill.bootzooka.huts
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.concurrent.Executors
import cats.effect._
import cats.data.NonEmptyList
import cats.implicits._
import com.softwaremill.bootzooka.http.Http
import monix.eval.Task
import com.softwaremill.bootzooka.infrastructure.Json._
import com.softwaremill.bootzooka.util.ServerEndpoints
import com.typesafe.scalalogging.StrictLogging
import sttp.client._
import io.circe.syntax._
import com.softwaremill.bootzooka.util.SttpUtils._
import fs2.text
import monix.execution.ExecutionModel.AlwaysAsyncExecution
import monix.execution.Scheduler
import monix.execution.schedulers.AsyncScheduler
import org.http4s.multipart.Multipart
import monix.nio.text.UTF8Codec._
import monix.nio.file._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Observable, Pipe}
import org.http4s.EntityBody
import sttp.tapir.{CodecFormat, Schema, SchemaType}
import scala.concurrent.duration._

/**
  * Created by Ilya Volynin on 16.12.2019 at 12:12.
  */
case class HutsApi(http: Http, config: HutsConfig)(implicit sttpBackend: SttpBackend[Task, Nothing, Nothing]) extends StrictLogging {
  import http._
  import HutsApi._

  private val HutsPath = "huts"

  private val samplesEndpoint = baseEndpoint.post
    .in(HutsPath / "samples")
    .in(jsonBody[Samples_IN])
    .out(jsonBody[Samples_OUT])
    .serverLogic[Task] { data =>
    (for {
      r <- basicRequest.post(uri"${config.url}")
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
    .serverLogic[Task] {
    hb =>
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
    .serverLogic[Task] {
    hf =>
      (for {
        r <- Task.now(("attachment; filename=resp-file.jpg",
          "application/octet-stream",
          Array(1.toByte, 2.toByte, 3.toByte)))
      } yield r).toOut
  }

  // endpoint.get.in("receive").out(streamBody[Source[ByteString, Any]](schemaFor[String], CodecFormat.TextPlain()))
  import fs2._
  private val streamingEndpoint = baseEndpoint.get
    .in(HutsPath / "stream")
    .out(header[String]("Content-Disposition"))
    .out(streamBody[EntityBody[Task]](schemaFor[Byte], CodecFormat.OctetStream()))
    .serverLogic[Task] {
    _ =>
      Stream.emit(List[Char]('a', 'b', 'c', 'd')).repeat.flatMap(list => Stream.chunk(Chunk.seq(list)))
      .take(10000000).covary[Task].map(_.toByte).pure[Task]
        .map(s => ("attachment; filename=resp-stream-file.txt", s)).toOut
  }

  val endpoints: ServerEndpoints =
    NonEmptyList
      .of(
        samplesEndpoint,
        fileUploadEndpoint,
        fileRetrievalEndpoint,
        streamingEndpoint
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
