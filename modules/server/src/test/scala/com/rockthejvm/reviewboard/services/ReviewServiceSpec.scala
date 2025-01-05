package com.rockthejvm.reviewboard.services

import zio.test._
import zio._
import com.rockthejvm.reviewboard.syntax._
import com.rockthejvm.reviewboard.repositories.ReviewRepositoryLive
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.repositories.ReviewRepository
import com.rockthejvm.reviewboard.domain.data.Review

object ReviewServiceSpec extends ZIOSpecDefault {

  val rtjvmReviewReq = CreateReviewRequest(1L, 1L, 1L, 5, 5, 5, 5, 1, "Awesome company")

  val stubRepoLayer = ZLayer {
    ZIO.succeed(new ReviewRepository {
      val db = collection.mutable.Map[Long, Review]()

      override def create(review: Review): Task[Review] =
        ZIO.succeed {
          val nextId    = db.keys.maxOption.getOrElse(0L) + 1
          val newReview = review.copy(id = nextId)
          db += (nextId -> newReview)
          newReview
        }

      override def update(id: Long, op: Review => Review): Task[Review] =
        ZIO.attempt {
          val review = db(id)
          db += (id -> op(review))
          review
        }

      override def delete(id: Long): Task[Review] =
        ZIO.attempt {
          val review = db(id)
          db -= id
          review
        }

      override def getById(id: Long): Task[Option[Review]] =
        ZIO.attempt(db.get(id))

      override def get: Task[List[Review]] = ZIO.succeed(db.values.toList)
    })
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewService")(
      test("create service") {
        val program = for {
          service <- ZIO.service[ReviewService]
          review  <- service.create(rtjvmReviewReq)
        } yield review

        program.assert { review =>
          review.id == rtjvmReviewReq.id &&
          review.companyId == rtjvmReviewReq.companyId &&
          review.userId == rtjvmReviewReq.userId
        }
      },
      test("get service by id") {
        val program = for {
          service     <- ZIO.service[ReviewService]
          review      <- service.create(rtjvmReviewReq)
          fetchedById <- service.getById(review.id)
        } yield fetchedById

        program.assert {
          case Some(review) => {
            review.id == rtjvmReviewReq.id &&
            review.companyId == rtjvmReviewReq.companyId &&
            review.userId == rtjvmReviewReq.userId
          }
          case _ => false
        }
      },
      test("get all services") {
        val program = for {
          service     <- ZIO.service[ReviewService]
          review      <- service.create(rtjvmReviewReq)
          allServices <- service.getAll
        } yield allServices

        program.assert(_.nonEmpty)
      }
    ).provide(ReviewServiceLive.layer, stubRepoLayer)
}
