package com.rockthejvm.reviewboard.core

import zio._
import com.raquo.airstream.eventbus.EventBus
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.client3.impl.zio.FetchZioBackend
import sttp.client3._
import sttp.tapir.Endpoint

object ZJS {

  def useBackend =
    ZIO.serviceWithZIO[BackendClient]

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A])
    def emitTo(eventBus: EventBus[A]) =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.fork(
          zio
            .tap(value => ZIO.attempt(eventBus.emit(value)))
            .provide(BackendClientLive.configuredLayer)
        )
      }

      // Endpoint[Unit, Unit, Throwable, List[Company], Any]
  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])
    def apply(payload: I): Task[O] =
      ZIO
        .service[BackendClient]
        .flatMap { backendClient =>
          backendClient.endpointRequestZIO(endpoint)(payload)
        }
        .provide(BackendClientLive.configuredLayer)

}
