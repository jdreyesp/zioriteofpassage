package com.rockthejvm.reviewboard.http.controllers

import zio._
import zio.test._
import sttp.client3._
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.client3.testing.SttpBackendStub
import zio.json._
import sttp.monad.MonadError
import sttp.tapir.ztapir.RIOMonadError
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.syntax._
import com.rockthejvm.reviewboard.services.CompanyService

object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  private val rtjvm = Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")
  private val serviceStub = new CompanyService {
    override def create(req: CreateCompanyRequest): Task[Company] = ZIO.succeed(rtjvm)
    override def getAll: Task[List[Company]]                      = ZIO.succeed(List(rtjvm))
    override def getById(id: Long): Task[Option[Company]] = ZIO.succeed {
      if (id == 1) Some(rtjvm)
      else None
    }
    override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed {
      if (slug == rtjvm.slug) Some(rtjvm)
      else None
    }
  }
  private def backendStubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) = for {
    // create the controller
    controller <- CompanyController.makeZIO
    // build tapir backend
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(zioMonadError))
        .whenServerEndpointRunLogic(endpointFun(controller))
        .backend()
    )
  } yield (backendStub)

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("CompanyControllerSpec")(
    test("post company") {
      val program = for {
        backendStub <- backendStubZIO(_.create)
        // run http request
        response <- basicRequest
          .post(uri"/companies")
          .body(CreateCompanyRequest("Rock the JVM", "rockthejvm.com").toJson)
          .send(backendStub)
      } yield response.body

      // inspect http response
      program.assert { respBody =>
        respBody.toOption
          .flatMap(_.fromJson[Company].toOption)
          .contains(Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))
      }

    },
    test("get all") {
      val program = for {
        backendStub <- backendStubZIO(_.getAll)
        response <- basicRequest
          .get(uri"/companies")
          .send(backendStub)
      } yield response.body

      // inspect http response
      program.assert { respBody =>
        respBody.toOption
          .flatMap(_.fromJson[List[Company]].toOption)
          .contains(List(rtjvm))
      }

    },
    test("get by id") {
      val program = for {
        // build tapir backend
        backendStub <- backendStubZIO(_.getById)
        response <- basicRequest
          .get(uri"/companies/1")
          .send(backendStub)
      } yield response.body

      // inspect http response
      program.assert { respBody =>
        respBody.toOption
          .flatMap(_.fromJson[Company].toOption)
          .contains(rtjvm)
      }

    }
  ).provide(ZLayer.succeed(serviceStub))
}
