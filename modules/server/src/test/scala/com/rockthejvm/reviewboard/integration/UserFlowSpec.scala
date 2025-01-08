package com.rockthejvm.reviewboard.integration

import zio._
import zio.test._
import com.rockthejvm.reviewboard.http.controllers.UserController
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.ztapir.RIOMonadError
import com.rockthejvm.reviewboard.services.UserServiceLive
import com.rockthejvm.reviewboard.services.JWTServiceLive
import com.rockthejvm.reviewboard.config.Configs.makeConfigLayer
import com.rockthejvm.reviewboard.config.JWTConfig
import com.rockthejvm.reviewboard.repositories.UserRepositoryLive
import sttp.client3.testing.SttpBackendStub
import sttp.client3._
import zio.json._
import com.rockthejvm.reviewboard.http.requests.RegisterUserAccount
import com.rockthejvm.reviewboard.repositories.Repositories
import com.rockthejvm.reviewboard.repositories.Repository
import zio.Scope
import com.rockthejvm.reviewboard.http.responses.UserResponse
import com.rockthejvm.reviewboard.http.requests.LoginRequest
import com.rockthejvm.reviewboard.services.UserServiceSpec.user
import com.rockthejvm.reviewboard.domain.data.UserToken
import sttp.model.Method
import com.rockthejvm.reviewboard.http.requests.UpdatePasswordRequest
import com.rockthejvm.reviewboard.http.requests.DeleteAccountRequest
import com.rockthejvm.reviewboard.services.UserService
import com.rockthejvm.reviewboard.repositories.UserRepository

object UserFlowSpec extends ZIOSpecDefault with Repositories {
  // http controller
  // service
  // repository
  // test container

  override val initScript: String = "sql/integration.sql"

  private val userRequest = RegisterUserAccount("daniel@rockthejvm.com", "rockthejvm")

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  private def backendStubZIO() =
    for {
      controller <- UserController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(zioMonadError))
          .whenServerEndpointsRunLogic(controller.routes)
          .backend()
      )
    } yield backendStub

  extension [A: JsonCodec](backend: SttpBackend[Task, Nothing]) {
    def sendRequest[B: JsonCodec](
        method: Method,
        path: String,
        payload: A,
        maybeToken: Option[String] = None
    ): Task[Option[B]] = {
      basicRequest
        .method(method, uri"$path")
        .body(payload.toJson)
        .auth
        .bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body)
        .map(_.toOption.flatMap(payload => payload.fromJson[B].toOption))
    }

    def post[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, None)

    def postAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, Some(token))

    def put[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, None)

    def putAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, Some(token))

    def delete[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, None)

    def deleteAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, Some(token))
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserFlowSpec")(
      test("create user") {
        for {
          backendStub <- backendStubZIO()
          maybeResponse <- backendStub
            .post[UserResponse](
              "/users",
              userRequest
            )
        } yield assertTrue(maybeResponse.contains(UserResponse(userRequest.email)))
      },
      test("create and log in") {
        for {
          backendStub <- backendStubZIO()
          maybeResponse <- backendStub
            .post[UserResponse](
              "/users",
              userRequest
            )
          maybeToken <- backendStub
            .post[UserToken](
              "/users/login",
              LoginRequest(userRequest.email, userRequest.password)
            )
        } yield assertTrue(maybeToken.filter(_.email == userRequest.email).nonEmpty)
      },
      test("change password") {
        for {
          backendStub <- backendStubZIO()
          maybeResponse <- backendStub
            .post[UserResponse](
              "/users",
              userRequest
            )
          token <- backendStub
            .post[UserToken](
              "/users/login",
              LoginRequest(userRequest.email, userRequest.password)
            )
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub.putAuth[UserResponse](
            "/users/password",
            UpdatePasswordRequest("daniel@rockthejvm.com", "rockthejvm", "scalarulez"),
            token.token
          )
          maybeOldToken <- backendStub
            .post[UserToken](
              "/users/login",
              LoginRequest(userRequest.email, userRequest.password)
            )
          maybeNewToken <- backendStub
            .post[UserToken](
              "/users/login",
              LoginRequest(userRequest.email, "scalarulez")
            )
        } yield assertTrue(maybeOldToken.isEmpty && maybeNewToken.nonEmpty)
      },
      test("delete user") {
        for {
          backendStub <- backendStubZIO()
          userRepo <- ZIO.service[
            UserRepository
          ] // Good thing is because of the ZLayering, I can inspect the users from, for instance, the repo layer
          maybeResponse <- backendStub
            .post[UserResponse](
              "/users",
              userRequest
            )
          maybeOldUser <- userRepo.getByEmail(userRequest.email)
          token <- backendStub
            .post[UserToken](
              "/users/login",
              LoginRequest(userRequest.email, userRequest.password)
            )
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub.deleteAuth[UserResponse](
            "/users",
            DeleteAccountRequest("daniel@rockthejvm.com", "rockthejvm"),
            token.token
          )
          maybeUser <- userRepo.getByEmail(userRequest.email)
        } yield assertTrue(
          maybeOldUser.filter(_.email == userRequest.email).nonEmpty && maybeUser.isEmpty
        )
      }
    ).provide(
      UserServiceLive.layer,
      UserRepositoryLive.layer,
      JWTServiceLive.layer,
      ZLayer.succeed(JWTConfig("secret", 3600)),
      Repository.quillLayer,
      dataSourceLayer,
      Scope.default
    )
}
