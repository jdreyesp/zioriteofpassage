package com.rockthejvm.reviewboard.http.controllers

import zio._
import com.rockthejvm.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.domain.data.UserID
import com.rockthejvm.reviewboard.services.JWTService

class ReviewController private (service: ReviewService, jwtService: JWTService)
    extends BaseController
    with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
      .serverLogic(userId => req => service.create(req).either)

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogic(_ => service.getAll.either)

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic(id => service.getById(id.toLong).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)

}

object ReviewController {
  def makeZIO =
    for {
      reviewService <- ZIO.service[ReviewService]
      jwtService    <- ZIO.service[JWTService]
    } yield new ReviewController(reviewService, jwtService)
}
