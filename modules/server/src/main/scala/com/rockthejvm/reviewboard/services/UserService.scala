package com.rockthejvm.reviewboard.services
import zio._
import com.rockthejvm.reviewboard.domain.data.User
import com.rockthejvm.reviewboard.repositories.UserRepository
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.repositories.RecoveryTokensRepository

trait UserService {
  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def deleteUser(email: String, password: String): Task[User]
  // JWT (access token used in the authentication bearer header in the HTTP calls)
  def generateToken(email: String, password: String): Task[Option[UserToken]]
  // password recovery flow
  def sendPasswordRecoveryToken(email: String): Task[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): Task[Boolean]
}

class UserServiceLive private (
    jwtService: JWTService,
    emailService: EmailService,
    userRepo: UserRepository,
    tokenRepo: RecoveryTokensRepository
) extends UserService {

  override def registerUser(email: String, password: String): Task[User] =
    userRepo.create(
      User(
        id = -1L, // generated by DB
        email = email,
        hashedPassword = UserServiceLive.Hasher.generateHash(password)
      )
    )

  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
      result <- existingUser match {
        case Some(user) =>
          ZIO
            .attempt(
              UserServiceLive.Hasher.validateHash(password, user.hashedPassword)
            )
            .orElseSucceed(false)
        case None => ZIO.succeed(false)
      }
    } yield result

  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] =
    for {
      user <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"User with email $email not found"))
      verified <- ZIO.attempt(UserServiceLive.Hasher.validateHash(oldPassword, user.hashedPassword))
      updatedUser <- userRepo
        .update(user.id, _.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword)))
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not update user with email: $email"))
    } yield updatedUser

  override def deleteUser(email: String, password: String): Task[User] =
    for {
      user <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"User with email $email not found!"))
      verified <- ZIO.attempt(UserServiceLive.Hasher.validateHash(password, user.hashedPassword))
      deletedUser <- userRepo
        .delete(user.id)
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not delete user with email: ${user.email}"))
    } yield deletedUser

  override def generateToken(email: String, password: String): Task[Option[UserToken]] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"cannot verify user $email"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      maybeToken <- jwtService.createToken(existingUser).when(verified)

    } yield maybeToken

  override def sendPasswordRecoveryToken(email: String): Task[Unit] = {
    // get a token from the token repo
    tokenRepo.getToken(email).flatMap { case Some(token) =>
      emailService.sendPasswordRecoveryEmail(email, token)
      case None => ZIO.unit
    }
  }

  override def recoverPasswordFromToken(
      email: String,
      token: String,
      newPassword: String
  ): Task[Boolean] = 
    for {
      existingUser <- userRepo.getByEmail(email).someOrFail(new RuntimeException("Non existent user"))
      tokenIsValid <- tokenRepo.checkToken(email, token)
      result <- userRepo
                .update(existingUser.id, user => user.copy(
                  id = user.id,
                  email = user.email,
                  hashedPassword = UserServiceLive.Hasher.generateHash(newPassword)
                ))
                .when(tokenIsValid)
                .map(_.nonEmpty)
    } yield result
}

object UserServiceLive {
  val layer = ZLayer {
    for {
      jwtService   <- ZIO.service[JWTService]
      emailService <- ZIO.service[EmailService]
      userRepo     <- ZIO.service[UserRepository]
      tokenRepo    <- ZIO.service[RecoveryTokensRepository]
    } yield new UserServiceLive(jwtService, emailService, userRepo, tokenRepo)
  }

  object Hasher {
    // with Hash based message authentication code (Hmac) with the crypto algorithm SHA512
    private val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS: Int   = 1000
    private val SALT_BYTE_SIZE: Int      = 24
    private val HASH_BYTE_SIZE: Int      = 24
    val skf: SecretKeyFactory            = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    private def pbkdf2(
        message: Array[Char],
        salt: Array[Byte],
        iterations: Int,
        nBytes: Int
    ): Array[Byte] = {
      val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, nBytes * 8)
      skf.generateSecret(keySpec).getEncoded()
    }

    // We are going to use the hex representation of an array of bytes since, once we store the hashed strings into the DB, that can lead to
    // characters that the RDBMS cannot parse correctly.
    // e.g. hex representation of the array of bytes: 2F 4A 56 (so 2F represents a byte, 4A represents another byte, so on and so forth)
    private def toHex(array: Array[Byte]): String =
      array.map(b => "%02X".format(b)).mkString

    private def fromHex(string: String): Array[Byte] =
      string.sliding(2, 2).toArray.map { hexValue =>
        Integer.parseInt(hexValue, 16).toByte
      }

      // comparison will be based on XOR of a(i) ^ b(i) for every i, that is,
    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean = {
      val range = 0 until math.min(a.length, b.length)
      val diff = range.foldLeft(a.length ^ b.length) { case (acc, i) =>
        acc | (a(i) ^ b(i))
      }
      diff == 0
    }

    // using java.security library
    // string + salt (random generated string) + nIterations with the algorithm PBKDF2 (password based key derivation function 2)
    // Representation: "1000:AAAAAAAAAAAAAAA:BBBBBBBBBBB"
    def generateHash(string: String): String = {
      val rng: SecureRandom = new SecureRandom()
      val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt) // creates 24 random bytes
      val hashBytes = pbkdf2(
        string.toCharArray(),
        salt,
        PBKDF2_ITERATIONS,
        HASH_BYTE_SIZE
      ) // I need to have the HASH_BYTE_SIZE, otherwise idk how long is the hash (unpredictable from the pbkdf2 algorithm)
      s"$PBKDF2_ITERATIONS:${{ toHex(salt) }}:${{ toHex(hashBytes) }}"
    }

    def validateHash(string: String, hash: String): Boolean = {
      val hashSegments = hash.split(":")
      val nIterations  = hashSegments(0).toInt
      val salt         = fromHex(hashSegments(1))
      val validHash    = fromHex(hashSegments(2))
      val testHash     = pbkdf2(string.toCharArray(), salt, nIterations, HASH_BYTE_SIZE)
      compareBytes(testHash, validHash)
    }
  }
}

object UserServiceDemo extends App {
  println(UserServiceLive.Hasher.generateHash("rockthejvm"))
  println(
    UserServiceLive.Hasher.validateHash(
      "rockthejvm",
      "1000:8A7935798A1CEB156EC94AE389716A918F98C425CBB736A6:D37E1F6EF9D76A370805AFFE1049BF2A26803D414DE1A883"
    )
  )
}
