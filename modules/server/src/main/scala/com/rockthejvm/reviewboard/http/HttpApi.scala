package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.HealthController
import com.rockthejvm.reviewboard.http.controllers.CompanyController
import com.rockthejvm.reviewboard.http.controllers.BaseController
import sttp.tapir.server.ServerEndpoint
import zio._

// The sole responsibility of this object is to keep track of all controllers, so that the Application
// does not need to know about them
object HttpApi {
  private def gatherRoutes(controllers: List[BaseController]): List[ServerEndpoint[Any, Task]] =
    controllers.flatMap(_.routes)

  private def makeControllers() = for {
    health    <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
  } yield List(health, companies)

  // val endpointsZIO = makeControllers().map(gatherRoutes)
  lazy val endpointsZIO = for {
    controllers <- makeControllers()
  } yield (gatherRoutes(controllers))
}
