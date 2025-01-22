package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir._
import sttp.tapir.json.zio._
import sttp.tapir.generic.auto._
import sttp.tapir.EndpointIO.annotations.jsonbody
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.data.CompanyFilter

trait CompanyEndpoints extends BaseEndpoint {
  val createEndpoint =
    secureBaseEndpoint
      .tag("companies")
      .name("create")
      .description("Create a listing for a company")
      .in("companies")
      .post
      .in(jsonBody[CreateCompanyRequest])
      .out(jsonBody[Company])

  val getAllEndpoint =
    baseEndpoint
      .tag("companies")
      .name("getAll")
      .description("get all company listings")
      .in("companies")
      .get
      .out(jsonBody[List[Company]])

  val getByIdEndpoint = baseEndpoint
    .tag("companies")
    .name("getById")
    .description("get company by its id (or maybe by its slug?)")
    .in("companies" / path[String])
    .get
    .out(jsonBody[Option[Company]])

  val allFiltersEndpoint = baseEndpoint
    .name("allFilters")
    .description("Get all possible search filters")
    .in("companies" / "filters")
    .get
    .out(jsonBody[CompanyFilter])

  val searchEndpoint = baseEndpoint
    .tag("companies")
    .name("search")
    .description("Get companies based on filters")
    .in("companies" / "search")
    .post
    .in(jsonBody[CompanyFilter])
    .out(jsonBody[List[Company]])
}
