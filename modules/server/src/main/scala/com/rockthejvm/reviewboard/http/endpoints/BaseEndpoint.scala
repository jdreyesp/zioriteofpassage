package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir._
import zio._
import com.rockthejvm.reviewboard.domain.errors.HttpError

trait BaseEndpoint {
  val baseEndpoint = endpoint
    .errorOut(statusCode and plainBody[String]) // (StatusCode, String)
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

  val secureBaseEndpoint = baseEndpoint
    .securityIn(auth.bearer[String]()) // header "Authorization Bearer ...."

}
