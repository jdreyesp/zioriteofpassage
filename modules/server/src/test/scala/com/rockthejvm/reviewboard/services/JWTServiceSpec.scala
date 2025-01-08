package com.rockthejvm.reviewboard.services

import zio._
import zio.test._
import com.rockthejvm.reviewboard.domain.data.User
import com.rockthejvm.reviewboard.config.JWTConfig

object JWTServiceSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("JWTServiceSpec")(
      test("create and validate token") {
        for {
          service <- ZIO.service[JWTService]
          token   <- service.createToken(User(1L, "daniel@rockthejvm.com", "unimportant"))
          userId  <- service.verifyToken(token.token)
        } yield assertTrue(
          userId.id == 1L &&
            userId.email == "daniel@rockthejvm.com"
        )
      }
    ).provide(JWTServiceLive.layer, ZLayer.succeed(JWTConfig("secret", 3600)))
}
