package com.rockthejvm.reviewboard.http.controllers

import zio._
import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._
import com.rockthejvm.reviewboard.domain.errors.HttpError

// We make the constructor private because of the reason explained below
class HealthController private extends BaseController with HealthEndpoint {

  val health: ServerEndpoint[Any, Task] =
    healthEndpoint.serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

    // .serverLogic will return an either of error or success
  val error: ServerEndpoint[Any, Task] =
    errorEndpoint
      .serverLogic[Task](_ => ZIO.fail(new RuntimeException("Boom!")).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(health, error)
}

object HealthController {
  // We create this factory method in order to control effectful creation of the controllers.
  // The controllers could have side effects / dependencies to other stuff (db, etc.) so we prefer
  // to keep it effectful.
  val makeZIO = ZIO.succeed(new HealthController())
}
