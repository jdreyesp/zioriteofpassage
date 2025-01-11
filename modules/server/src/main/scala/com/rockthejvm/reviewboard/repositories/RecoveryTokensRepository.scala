package com.rockthejvm.reviewboard.repositories

import zio._
import io.getquill.jdbczio.Quill
import io.getquill._
import com.rockthejvm.reviewboard.domain.data.PasswordRecoveryToken
import com.rockthejvm.reviewboard.config.RecoveryTokensConfig
import com.rockthejvm.reviewboard.config.Configs

trait RecoveryTokensRepository {
  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]
}

class RecoveryTokensRepositoryLive private (
    tokenConfig: RecoveryTokensConfig,
    quill: Quill.Postgres[SnakeCase],
    userRepo: UserRepository
) extends RecoveryTokensRepository {

  import quill._

  inline given schema: SchemaMeta[PasswordRecoveryToken] =
    schemaMeta[PasswordRecoveryToken]("recovery_token") // recovery_token is the name of the table
  inline given insMeta: InsertMeta[PasswordRecoveryToken] = insertMeta[PasswordRecoveryToken]()
  inline given upMeta: UpdateMeta[PasswordRecoveryToken] =
    updateMeta[PasswordRecoveryToken](_.email) // PK

  private val tokenDuration = 600 // TODO pass this from config

  override def getToken(email: String): Task[Option[String]] = {
    // check the user in the db
    // if the user exists, make a fresh one
    userRepo.getByEmail(email).flatMap {
      case None    => ZIO.none
      case Some(_) => makeFreshToken(email).map(Some(_))
    }
  }

  private def randomUppercaseString(len: Int): Task[String] =
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase())

  private def findToken(email: String): Task[Option[String]] =
    run(query[PasswordRecoveryToken].filter(_.email == lift(email))).map(_.headOption.map(_.token))

  private def replaceToken(email: String): Task[String] = {
    for {
      token <- randomUppercaseString(8)
      _ <- run(
        query[PasswordRecoveryToken]
          .updateValue(
            lift(
              PasswordRecoveryToken(
                email,
                token,
                java.lang.System.currentTimeMillis() + tokenDuration
              )
            )
          )
          .returning(r => r)
      )
    } yield token
  }

  private def generateToken(email: String): Task[String] = for {
    token <- randomUppercaseString(8)
    _ <- run(
      query[PasswordRecoveryToken]
        .insertValue(
          lift(
            PasswordRecoveryToken(
              email,
              token,
              java.lang.System.currentTimeMillis() + tokenDuration
            )
          )
        )
        .returning(r => r)
    )
  } yield token

  private def makeFreshToken(email: String): Task[String] = {
    // find token in the table
    // if so, replace
    // if not, create
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case None    => generateToken(email)
    }
  }
  override def checkToken(email: String, token: String): Task[Boolean] = run(
    query[PasswordRecoveryToken]
      .filter(r => r.email == lift(email) && r.token == lift(token))
      .take(1)
  ).map(_.nonEmpty)
}

object RecoveryTokensRepositoryLive {
  val layer = ZLayer {
    for {
      config   <- ZIO.service[RecoveryTokensConfig]
      quill    <- ZIO.service[Quill.Postgres[SnakeCase]]
      userRepo <- ZIO.service[UserRepository]
    } yield new RecoveryTokensRepositoryLive(config, quill, userRepo)
  }

  val configuredLayer =
    Configs.makeConfigLayer[RecoveryTokensConfig]("rockthejvm.recoverytokens") >>> layer
}
