package com.rockthejvm.reviewboard.http.controllers

import zio._
import com.rockthejvm.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints

class ReviewController private (service: ReviewService)
    extends BaseController
    with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogic(req => service.create(req).either)

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogic(_ => service.getAll.either)

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic(id => service.getById(id.toLong).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)

}

object ReviewController {
  def makeZIO = ZIO.service[ReviewService].map(reviewService => new ReviewController(reviewService))
}
