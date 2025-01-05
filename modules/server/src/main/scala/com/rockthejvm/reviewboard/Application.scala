package com.rockthejvm.reviewboard

import zio._
import sttp.tapir._
import zio.http.Server
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions
import com.rockthejvm.reviewboard.http.controllers.HealthController
import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.services.CompanyService
import com.rockthejvm.reviewboard.services.CompanyServiceLive
import com.rockthejvm.reviewboard.repositories._
import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase
import com.rockthejvm.reviewboard.services.ReviewServiceLive

object Application extends ZIOAppDefault {

  val serverProgram = (for {
    endpoints <- HttpApi.endpointsZIO
    _         <- Console.printLine(s"Loaded endpoints: $endpoints")
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default // can add configs e.g. CORS
      ).toHttp(endpoints)
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ())

  def run = serverProgram.provide(
    Server.default,
    // Services
    CompanyServiceLive.layer,
    ReviewServiceLive.layer,

    // Repositories
    CompanyRepositoryLive.layer,
    ReviewRepositoryLive.layer,

    // Other requirements
    Repository.dataLayer
  )
}
