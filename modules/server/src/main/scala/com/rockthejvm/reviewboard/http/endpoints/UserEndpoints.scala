package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir._
import sttp.tapir.json.zio._
import sttp.tapir.generic.auto._
import sttp.tapir.EndpointIO.annotations.jsonbody
import com.rockthejvm.reviewboard.http.requests.RegisterUserAccount
import com.rockthejvm.reviewboard.http.responses.UserResponse
import com.rockthejvm.reviewboard.http.requests.UpdatePasswordRequest
import com.rockthejvm.reviewboard.http.requests.DeleteAccountRequest
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.http.requests.LoginRequest

trait UserEndpoints extends BaseEndpoint {

  val createUserEndpoint =
    baseEndpoint
      .tag("Users")
      .name("register")
      .description("Register a user account with username and password")
      .in("users")
      .post
      .in(jsonBody[RegisterUserAccount])
      .out(jsonBody[UserResponse])

  // TODO - should be an authorized endpoint (JWT)
  val updatePasswordEndpoint =
    secureBaseEndpoint
      .tag("Users")
      .name("updatePassword")
      .description("Update user password")
      .in("users" / "password")
      .put
      .in(jsonBody[UpdatePasswordRequest])
      .out(jsonBody[UserResponse])

  val deleteEndpoint =
    secureBaseEndpoint
      .tag("Users")
      .name("delete")
      .description("Delete user account")
      .delete
      .in(jsonBody[DeleteAccountRequest])
      .out(jsonBody[UserResponse])

  val loginEndpoint =
    baseEndpoint
      .tag("Users")
      .name("login")
      .description("Login and generate JWT token")
      .in("users" / "login")
      .post
      .in(jsonBody[LoginRequest])
      .out(jsonBody[UserToken])
}
