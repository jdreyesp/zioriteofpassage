package com.rockthejvm.reviewboard.http.controllers

import zio._
import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint

// We make the constructor private because of the reason explained below
class HealthController private extends HealthEndpoint {
  val health = healthEndpoint.serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))
}

object HealthController {
  // We create this factory method in order to control effectful creation of the controllers.
  // The controllers could have side effects / dependencies to other stuff (db, etc.) so we prefer
  // to keep it effectful.
  val makeZIO = ZIO.succeed(new HealthController)
}
