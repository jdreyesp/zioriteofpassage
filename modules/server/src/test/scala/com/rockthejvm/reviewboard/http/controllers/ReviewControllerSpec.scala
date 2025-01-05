package com.rockthejvm.reviewboard.http.controllers

import zio.test.ZIOSpecDefault
import zio._
import zio.json._
import zio.test.Spec
import zio.test.TestEnvironment

import com.rockthejvm.reviewboard.services.ReviewService
import com.rockthejvm.reviewboard.syntax._
import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest

import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import sttp.monad.MonadError
import sttp.client3.testing.SttpBackendStub
import sttp.client3._

import java.time.Instant

object ReviewControllerSpec extends ZIOSpecDefault {

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  val rtjvmReviewReq: CreateReviewRequest =
    CreateReviewRequest(1L, 1L, 1L, 5, 5, 5, 5, 1, "Awesome company", Instant.now(), Instant.now())

  val serviceStub = ZLayer {
    ZIO.succeed(new ReviewService {

      override def create(req: CreateReviewRequest): Task[Review] = ZIO.succeed(req.toReview())
      override def getAll: Task[List[Review]] = ZIO.succeed(List(rtjvmReviewReq.toReview()))
      override def getById(id: Long): Task[Option[Review]] =
        ZIO.succeed(Some(rtjvmReviewReq.toReview()))
    })
  }

  private def backendStubZIO(endpointFun: ReviewController => ServerEndpoint[Any, Task]) =
    for {
      controller <- ReviewController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(zioMonadError))
          .whenServerEndpointRunLogic(endpointFun(controller))
          .backend()
      )
    } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewController")(
      test("create review") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/reviews")
            .body(rtjvmReviewReq.toJson)
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(rtjvmReviewReq.toReview())
        }
      },
      test("get all reviews") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/reviews")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .contains(List(rtjvmReviewReq.toReview()))
        }
      },
      test("get review by id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/reviews/${rtjvmReviewReq.id}")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[Option[Review]].toOption)
            .contains(Some(rtjvmReviewReq.toReview()))
        }
      }
    ).provide(serviceStub)
}
