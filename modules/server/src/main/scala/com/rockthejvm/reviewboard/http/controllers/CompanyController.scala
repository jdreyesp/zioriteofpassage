package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import zio._
import collection.mutable
import com.rockthejvm.reviewboard.domain.data.Company
import sttp.tapir.server.ServerEndpoint
import scala.util.Try
import com.rockthejvm.reviewboard.services.CompanyService

class CompanyController private (service: CompanyService)
    extends BaseController
    with CompanyEndpoints {

  // create
  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogic { req =>
    service.create(req).either
  }

  // getAll
  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogic(_ => service.getAll.either)

  // getById
  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic { id =>
      ZIO
        .attempt(id.toLong)
        .flatMap(service.getById)
        .catchSome { case _: NumberFormatException => service.getBySlug(id) }
        .either
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)

}

object CompanyController {
  val makeZIO = for {
    service <- ZIO.service[CompanyService]
  } yield new CompanyController(service)
}
