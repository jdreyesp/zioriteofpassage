package com.rockthejvm.reviewboard.services

import zio._
import com.rockthejvm.reviewboard.domain.data.{User, UserID}
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.auth0.jwt.JWT
import java.time.Instant
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.rockthejvm.reviewboard.config.JWTConfig
import zio.config.typesafe.TypesafeConfig
import com.typesafe.config.ConfigFactory
import com.rockthejvm.reviewboard.config.Configs

/** Explanation on how JWT works below in the Demo object
  */
trait JWTService {
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserID]
}

class JWTServiceLive(jwtConfig: JWTConfig, clock: java.time.Clock) extends JWTService {

  private val ISSUER         = "rockthejvm.com"
  private val CLAIM_USERNAME = "username"

  private val algorithm = Algorithm.HMAC512(jwtConfig.secret)
  private val verifier: JWTVerifier =
    JWT
      .require(algorithm)
      .withIssuer(ISSUER)
      .asInstanceOf[BaseVerification]
      .build(clock)

  override def createToken(user: User): Task[UserToken] = for {
    now        <- ZIO.attempt(clock.instant())
    expiration <- ZIO.succeed(now.plusSeconds(jwtConfig.ttl))
    token <- ZIO.attempt(
      JWT
        .create()
        .withIssuer(ISSUER)
        .withIssuedAt(now)
        .withExpiresAt(expiration) // 30 days
        .withSubject(user.id.toString)
        .withClaim(CLAIM_USERNAME, user.email)
        .sign(algorithm)
    )
  } yield UserToken(user.email, token, expiration.getEpochSecond())

  override def verifyToken(token: String): Task[UserID] =
    for {
      decoded <- ZIO.attempt(verifier.verify(token))
      userId <- ZIO.attempt {
        UserID(decoded.getSubject().toLong, decoded.getClaim("username").asString())
      }
    } yield userId

}

object JWTServiceLive {
  val layer = ZLayer {
    for {
      jwtConfig <- ZIO.service[JWTConfig]
      clock     <- Clock.javaClock
    } yield new JWTServiceLive(jwtConfig, clock)
  }

  val configuredLayer = Configs.makeConfigLayer[JWTConfig]("rockthejvm.jwt") >>> layer
}

object JWTServiceDemo extends App {
  val algorithm = Algorithm.HMAC512("secret" /*seed*/ )
  val jwt = JWT
    .create()
    .withIssuer("rockthejvm.com")
    .withIssuedAt(Instant.now())
    .withExpiresAt(Instant.now().plusSeconds(3600 * 24 * 30)) // 30 days
    .withSubject("1")
    .withClaim(
      "username",
      "daniel@rockthejvm.com"
    ) // key-value pair where you can store permissions, access rights... any kind of data
    .sign(algorithm)
  println(jwt)
  // header
  // eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.
  // claim (issuer, issue time, expiration time, subject, custom claim, ...)
  // eyJzdWIiOiIxIiwiaXNzIjoicm9ja3RoZWp2bS5jb20iLCJleHAiOjE3Mzg4NTQ2MTYsImlhdCI6MTczNjI2MjYxNiwidXNlcm5hbWUiOiJkYW5pZWxAcm9ja3RoZWp2bS5jb20ifQ.
  // signature (all the claims + algorithm + seed (secret))
  // xyvJJsxEgas3Srsr2UtN4WYLwOzoaPN57IXotN4Y2sWl3yN6RkHRQIV0s503CWHzJXDf44H63q9Xj6AVCwi-KQ

  val verifier: JWTVerifier =
    JWT
      .require(algorithm)
      .withIssuer("rockthejvm.com")
      .asInstanceOf[BaseVerification]
      .build(java.time.Clock.systemDefaultZone())

  val decoded   = verifier.verify(jwt)
  val userId    = decoded.getSubject()
  val userEmail = decoded.getClaim("username").asString()
  println(userId)
  println(userEmail)
}

object JWTZIOServiceDemo extends ZIOAppDefault {
  val program = for {
    service <- ZIO.service[JWTService]
    token   <- service.createToken(User(1L, "daniel@rockthejvm.com", "unimportant"))
    _       <- Console.printLine(token)
    userId  <- service.verifyToken(token.token)
    _       <- Console.printLine(userId.toString)
  } yield ()

  def run = program.provide(
    JWTServiceLive.layer,
    Configs.makeConfigLayer[JWTConfig]("rockthejvm.jwt")
  )
}
