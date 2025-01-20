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
import com.rockthejvm.reviewboard.services.UserServiceLive
import com.rockthejvm.reviewboard.services.JWTServiceLive
import com.rockthejvm.reviewboard.config.Configs
import com.rockthejvm.reviewboard.config.JWTConfig
import com.rockthejvm.reviewboard.services.EmailServiceLive
import sttp.tapir.server.interceptor.cors.CORSInterceptor

object Application extends ZIOAppDefault {

  val serverProgram = (for {
    endpoints <- HttpApi.endpointsZIO
    _         <- Console.printLine(s"Loaded endpoints: $endpoints")
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default.appendInterceptor(
          CORSInterceptor.default
        )
      ).toHttp(endpoints)
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ())

  def run = serverProgram.provide(
    Server.default,

    // Services
    CompanyServiceLive.layer,
    ReviewServiceLive.layer,
    UserServiceLive.layer,
    JWTServiceLive.configuredLayer,
    EmailServiceLive.configuredLayer,

    // Repositories
    CompanyRepositoryLive.layer,
    ReviewRepositoryLive.layer,
    UserRepositoryLive.layer,
    RecoveryTokensRepositoryLive.configuredLayer,

    // Other requirements
    Repository.dataLayer
  )
}
