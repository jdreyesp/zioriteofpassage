package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import zio._
import collection.mutable
import com.rockthejvm.reviewboard.domain.data.Company
import sttp.tapir.server.ServerEndpoint
import scala.util.Try

class CompanyController private extends BaseController with CompanyEndpoints {

  // TODO implementations
  // in-memory 'database'

  val db = mutable.Map[Long, Company]()

  // create
  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    ZIO.succeed {
      // create an id
      val newId = Try(db.keys.max).getOrElse(0L) + 1
      // create a slug
      // create a company
      val company = req.toCompany(newId)
      // insert the company in the 'database'
      db += (newId -> company)
      // return that company
      company
    }
  }

  // getAll
  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogicSuccess(_ => ZIO.succeed(db.values.toList))

  // getById
  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess { id =>
      ZIO
        .attempt(id.toLong)
        .map(db.get)
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)

}

object CompanyController {
  val makeZIO = ZIO.succeed(new CompanyController())
}
