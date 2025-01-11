package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir._
import sttp.tapir.json.zio._
import sttp.tapir.generic.auto._
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.domain.data.Review

trait ReviewEndpoints extends BaseEndpoint {
  val createEndpoint =
    secureBaseEndpoint
      .tag("reviews")
      .name("create")
      .description("Create a review")
      .in("reviews")
      .post
      .in(jsonBody[CreateReviewRequest])
      .out(jsonBody[Review])

  val getAllEndpoint =
    baseEndpoint
      .tag("reviews")
      .name("getAll")
      .description("Get all reviews")
      .in("reviews")
      .get
      .out(jsonBody[List[Review]])

  val getByIdEndpoint =
    baseEndpoint
      .tag("reviews")
      .name("getById")
      .description("Get review by Id")
      .in("reviews" / path[String])
      .get
      .out(jsonBody[Option[Review]])
}
