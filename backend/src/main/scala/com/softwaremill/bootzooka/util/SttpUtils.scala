package com.softwaremill.bootzooka.util
import com.softwaremill.bootzooka.Fail
import com.softwaremill.bootzooka.Fail.NotFound
import io.circe.Decoder
import monix.eval.Task
import sttp.client.Response
import io.circe.parser.decode
import com.typesafe.scalalogging.{Logger, StrictLogging}

/**
  * Created by Ilya Volynin on 16.12.2019 at 15:21.
  */
object SttpUtils extends StrictLogging{

  def handleRemoteResponse[R](implicit dec: Decoder[R]): Response[Either[String, String]] => Task[R] = {
    res =>
      if (res.code.isSuccess) {
        res.body.fold(
          fa => {
            logger.error(s"respose code success {}, but body {}", res.code.toString(), res.body)
            Task.raiseError(NotFound(fa))
          },
          fb => decode[R](fb).fold(error => {
            logger.error(s"error decoding {}", error)
            Task.raiseError(Fail.IncorrectInput(fb))
          },
            l => Task.now(l)))
      } else {
        logger.error(s"unsuccessfull response, code {}, body ", res.code, res.body)
        Task.raiseError(Fail.IncorrectInput(s"unsuccessfull response ${res.code}"))
      }
  }
}
