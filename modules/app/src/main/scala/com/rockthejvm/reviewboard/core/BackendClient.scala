package com.rockthejvm.reviewboard.core

import sttp.model.Uri
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import sttp.client3.SttpBackend
import zio._
import sttp.capabilities.WebSockets
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.Endpoint
import sttp.client3._
import sttp.capabilities.zio.ZioStreams
import sttp.client3.impl.zio.FetchZioBackend
import com.rockthejvm.reviewboard.http.endpoints.UserEndpoints

case class BackendClientConfig(uri: Option[Uri])
trait BackendClient {
  // fetch API or
  // AJAX or
  // ZIO endpoints <-- we're doing this
  val companyEndpoints: CompanyEndpoints
  val userEndpoints: UserEndpoints

  def endpointRequest[I, E, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O], Any]

  def endpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(
      payload: I
  ): Task[O]

}

class BackendClientLive(
    backend: SttpBackend[Task, WebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig
) extends BackendClient {

  override val companyEndpoints: CompanyEndpoints = new CompanyEndpoints {}
  override val userEndpoints: UserEndpoints       = new UserEndpoints {}
  // val request = interpreter
  //   .toRequestThrowDecodeFailures(theEndpoint, Some(uri"http://localhost:8080"))
  //   .apply(())
  // val companiesZIO = backend.send(request).map(_.body).absolve

  override def endpointRequest[I, E, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O], Any] =
    interpreter
      .toRequestThrowDecodeFailures(endpoint, config.uri)

  override def endpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(
      payload: I
  ): Task[O] =
    backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve
}

object BackendClientLive {
  val layer = ZLayer {
    for {
      backend     <- ZIO.service[SttpBackend[Task, ZioStreams & WebSockets]]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[BackendClientConfig]
    } yield new BackendClientLive(backend, interpreter, config)
  }

  val configuredLayer: ZLayer[Any, Nothing, BackendClientLive] = {
    // localhost:8080
    val backend                            = FetchZioBackend()
    val interpreter: SttpClientInterpreter = SttpClientInterpreter()
    val config                             = BackendClientConfig(Some(uri"http://localhost:8080"))

    ZLayer.succeed(backend) ++
      ZLayer.succeed(interpreter) ++
      ZLayer.succeed(config) >>> layer
  }
}
