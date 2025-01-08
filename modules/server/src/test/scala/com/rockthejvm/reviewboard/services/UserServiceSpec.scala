package com.rockthejvm.reviewboard.services

import zio.test.ZIOSpecDefault
import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import com.rockthejvm.reviewboard.repositories.UserRepository

import zio._
import zio.test._
import com.rockthejvm.reviewboard.domain.data.User
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.domain.data.UserID

object UserServiceSpec extends ZIOSpecDefault {

  val user = User(
    1L,
    "daniel@rockthejvm.com",
    "1000:8A7935798A1CEB156EC94AE389716A918F98C425CBB736A6:D37E1F6EF9D76A370805AFFE1049BF2A26803D414DE1A883"
  )

  val stubRepoLayer = ZLayer.succeed {
    new UserRepository {
      val db = collection.mutable.Map[Long, User](1L -> user)

      override def create(user: User): Task[User] = ZIO.succeed {
        db += (user.id -> user)
        user
      }

      override def getById(id: Long): Task[Option[User]] = ZIO.succeed(db.get(id))

      override def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))

      override def update(id: Long, op: User => User): Task[User] = ZIO.attempt {
        val newUser = op(db(id))
        db += (newUser.id -> newUser)
        newUser
      }

      override def delete(id: Long): Task[User] = ZIO.attempt {
        val user = db(id)
        db -= id
        user
      }
    }
  }

  val stubJWTLayer = ZLayer.succeed {
    new JWTService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "BIG ACCESS", Long.MaxValue))
      override def verifyToken(token: String): Task[UserID] =
        ZIO.succeed(UserID(user.id, user.email))
    }
  }
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserServiceSpec")(
      test("create and validate a user") {
        for {
          service        <- ZIO.service[UserService]
          registeredUser <- service.registerUser(user.email, "rockthejvm")
          valid          <- service.verifyPassword(registeredUser.email, "rockthejvm")
        } yield assertTrue(valid && registeredUser.email == user.email)
      },
      test("validate correct credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(user.email, "rockthejvm")
        } yield assertTrue(valid)
      },
      test("invalidate incorrect creedentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(user.email, "incorrect")
        } yield assertTrue(!valid)
      },
      test("invalidate non-existent user") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("someone@gmail.com", "password")
        } yield assertTrue(!valid)
      },
      test("update password") {
        for {
          service  <- ZIO.service[UserService]
          newUser  <- service.updatePassword(user.email, "rockthejvm", "scalarulez")
          oldValid <- service.verifyPassword(user.email, "rockthejvm")
          newValid <- service.verifyPassword(user.email, "scalarulez")
        } yield assertTrue(!oldValid && newValid)
      },
      test("delete with non existing user should fail") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.deleteUser("someone@gmail.com", "something").flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete with incorrect credentials should fail") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.deleteUser(user.email, "incorrect").flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete user") {
        for {
          service     <- ZIO.service[UserService]
          deletedUser <- service.deleteUser(user.email, "rockthejvm")
        } yield assertTrue(deletedUser.email == user.email)
      }
    ).provide(
      UserServiceLive.layer,
      stubJWTLayer,
      stubRepoLayer
    )
}
