package com.example

import zio._
import scala.io.StdIn

object ZIORecap extends ZIOAppDefault {

  // ZIO = data structure describing arbitrary computation (including side effects)
  // effects = computations as values

  // basics
  val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  // fail
  val aFailure: ZIO[Any, String, Nothing] = ZIO.fail("Something went wrong")
  // suspension / delay
  val aSuspension: ZIO[Any, Throwable, Int] = ZIO.suspend(meaningOfLife)

  // map / flatMap
  val improvedMOL = meaningOfLife.map(_ * 2)
  val printingMOL = meaningOfLife.flatMap(mol => ZIO.succeed(println(mol)))
  val smallProgram = for {
    _    <- Console.printLine("What's your name?")
    name <- ZIO.succeed(StdIn.readLine())
    _    <- Console.printLine(s"Welcome to ZIO, $name")
  } yield ()

  // error handling
  val anAttempt: ZIO[Any, Throwable, Int] = ZIO.attempt {
    println("Trying something")
    val string: String = null
    string.length
  }

  // catch errors effectfully
  val catchError = anAttempt.catchAll(e => ZIO.succeed(s"Returning some different value"))
  val catchSelective = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runtime exception: $e")
    case _                   => ZIO.succeed("Ignoring everything else")
  }
  // There are many other APIs for error handling, error type broadening / restriction, conversion to Option, Try, ...

  // fibers
  val delayedValue = ZIO.sleep(1.second) *> Random.nextIntBetween(0, 100)
  val aPair = for {
    a <- delayedValue
    b <- delayedValue
  } yield (a, b) // this takes 2 seconds

  val aPairPar = for {
    fibA <- delayedValue.fork
    fibB <- delayedValue.fork
    a    <- fibA.join
    b    <- fibB.join
  } yield (a, b) // this takes 1 seconds

  val interruptedFiber = for {
    fib <- delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted"))).fork
    _   <- ZIO.sleep(500.millis) *> ZIO.succeed(println("cancelling fiber")) *> fib.interrupt
    _   <- fib.join
  } yield ()

  val ignoredInterruption = for {
    fib <- ZIO
      .uninterruptible(
        delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted")))
      )
      .fork
    _     <- ZIO.sleep(500.millis) *> ZIO.succeed(println("cancelling fiber")) *> fib.interrupt
    value <- fib.join
  } yield value

  // many APIs on top of fibers
  val aPairPar_v2 = delayedValue.zipPar(delayedValue)
  val randomx10   = ZIO.collectAllPar((1 to 10).map(_ => delayedValue)) // "traverse"
  // reduceAllPar, mergerAllPar, foreachPar, ...

  // dependencies
  case class User(name: String, email: String)
  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
    def subscribeUser(user: User): Task[Unit] = for {
      _ <- emailService.email(user)
      _ <- userDatabase.insert(user)
      _ <- ZIO.succeed(s"subscribed $user")
    } yield ()
  }
  object UserSubscription {
    val live: ZLayer[EmailService & UserDatabase, Nothing, UserSubscription] =
      ZLayer.fromFunction((emailS, userD) => new UserSubscription(emailS, userD))
  }
  class EmailService {
    def email(user: User): Task[Unit] = ZIO.succeed(s"Emailed $user")
  }
  object EmailService {
    val live: ZLayer[Any, Nothing, EmailService] = ZLayer.succeed(new EmailService())
  }
  class UserDatabase(connectionPool: ConnectionPool) {
    def insert(user: User): Task[Unit] = ZIO.succeed(s"Inserted $user")
  }
  object UserDatabase {
    val live: ZLayer[ConnectionPool, Nothing, UserDatabase] =
      ZLayer.fromFunction(new UserDatabase(_))
  }
  class ConnectionPool(nConnections: Int) {
    def get: Task[Connection] = ZIO.succeed(Connection())
  }
  object ConnectionPool {
    def live(nConnections: Int): ZLayer[Any, Nothing, ConnectionPool] =
      ZLayer.succeed(ConnectionPool(nConnections))
  }
  case class Connection()

  def subscribe(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
    sub <- ZIO.service[UserSubscription] // extracts the R
    _   <- sub.subscribeUser(user)
  } yield ()

  val program = for {
    _ <- subscribe(User("daniel", "daniel@rockthejvm.com"))
    _ <- subscribe(User("diego", "diego@rockthejvm.com"))
  } yield ()

  // def run = Console.printLine("Rock the JVM")
  def run = program.provide(
    ConnectionPool.live(10), // Build me a connectionPool
    UserDatabase.live,       // build me a userDatabase, using the connectionPool
    EmailService.live,       // build me an EmailService
    UserSubscription.live    // build me a UserSubscription
  )
}
