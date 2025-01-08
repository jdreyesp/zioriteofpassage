package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.http.endpoints.UserEndpoints
import sttp.tapir._
import sttp.tapir.server._
import zio._
import com.rockthejvm.reviewboard.services.UserService
import com.rockthejvm.reviewboard.http.responses.UserResponse
import com.rockthejvm.reviewboard.domain.errors.UnauthorizedException
import com.rockthejvm.reviewboard.services.JWTService
import com.rockthejvm.reviewboard.services.JWTServiceDemo.jwt
import com.rockthejvm.reviewboard.domain.data.UserID
import com.stripe.param.ChargeUpdateParams.FraudDetails.UserReport
import com.rockthejvm.reviewboard.services.JWTServiceLive

class UserController private (userService: UserService, jwtService: JWTService)
    extends BaseController
    with UserEndpoints {

  val create: ServerEndpoint[Any, Task] = createUserEndpoint
    .serverLogic { req =>
      userService
        .registerUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  val login: ServerEndpoint[Any, Task] = loginEndpoint
    .serverLogic { req =>
      userService
        .generateToken(req.email, req.password)
        .someOrFail(UnauthorizedException)
        .either
    }

  // change password - check for JWT
  val updatePassword: ServerEndpoint[Any, Task] = updatePasswordEndpoint
    .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { userId => req =>
      userService
        .updatePassword(req.email, req.oldPassword, req.newPassword)
        .map(user => UserResponse(user.email))
        .either
    }

  val delete: ServerEndpoint[Any, Task] = deleteEndpoint
    .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { userId => req =>
      userService
        .deleteUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, updatePassword, delete, login)

}

object UserController {
  def makeZIO = for {
    userService <- ZIO.service[UserService]
    jwtService  <- ZIO.service[JWTService]
  } yield UserController(userService, jwtService)
}
