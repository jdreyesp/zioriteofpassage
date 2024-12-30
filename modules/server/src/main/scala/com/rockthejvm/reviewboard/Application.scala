package com.rockthejvm.reviewboard

import zio._
import sttp.tapir._
import zio.http.Server
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions
import com.rockthejvm.reviewboard.http.controllers.HealthController

object Application extends ZIOAppDefault {

  val serverProgram = for {
    controller <- HealthController.makeZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default // can add configs e.g. CORS
      ).toHttp(controller.health)
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ()

  def run = serverProgram.provide(Server.default)
}
