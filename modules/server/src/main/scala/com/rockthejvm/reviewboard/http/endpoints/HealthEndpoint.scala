package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir._

trait HealthEndpoint extends BaseEndpoint {

  val healthEndpoint = baseEndpoint
    .tag("health")
    .name("health")
    .description("health endpoint possible")
    // ^^ for documentation
    .get                    // HTTP method
    .in("health")           // path
    .out(plainBody[String]) // output

  val errorEndpoint = baseEndpoint
    .tag("health")
    .name("error health")
    .description("health check - should fail")
    // ^^ for documentation
    .get                    // HTTP method
    .in("health" / "error") // path
    .out(plainBody[String]) // output
}
