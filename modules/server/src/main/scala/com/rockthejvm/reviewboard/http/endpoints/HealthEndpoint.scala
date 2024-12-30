package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir._

trait HealthEndpoint {

  val healthEndpoint = endpoint
    .tag("health")
    .name("health")
    .description("health endpoint possible")
    // ^^ for documentation
    .get                    // HTTP method
    .in("health")           // path
    .out(plainBody[String]) // output

}
