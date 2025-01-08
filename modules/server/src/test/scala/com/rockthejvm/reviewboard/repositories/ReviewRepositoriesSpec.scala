package com.rockthejvm.reviewboard.repositories

import zio.test.ZIOSpecDefault
import zio._
import zio.test._
import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.syntax._
import java.time.Instant

object ReviewRepositoriesSpec extends ZIOSpecDefault with Repositories {

  override val initScript: String = "sql/reviews.sql"

  private val rtjvmReview =
    Review(1L, -1L, -1L, 10, 10, 10, 10, 1, "Best company ever", Instant.now(), Instant.now())

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Review repository")(
      test("create a review") {
        val program = for {
          reviewRepo <- ZIO.service[ReviewRepository]
          review     <- reviewRepo.create(rtjvmReview)
        } yield review

        program.assert { review =>
          review.id == rtjvmReview.id &&
          review.companyId == rtjvmReview.companyId &&
          review.userId == rtjvmReview.userId
        }

        /* Equivalent */
        // assertZIO(program)(
        //   Assertion.assertion("assertion")(review =>
        //     review.id == rtjvmReview.id &&
        //       review.companyId == rtjvmReview.companyId &&
        //       review.userId == rtjvmReview.userId
        //   )
        // )

      },
      test("Update a review") {
        val program = for {
          reviewRepo <- ZIO.service[ReviewRepository]
          _          <- reviewRepo.create(rtjvmReview)
          review     <- reviewRepo.update(1L, _.copy(companyId = 2L))
        } yield review

        program.assert { review =>
          review.id == rtjvmReview.id &&
          review.companyId == 2L &&
          review.userId == rtjvmReview.userId
        }
      },
      test("Get by ID") {
        val program = for {
          reviewRepo <- ZIO.service[ReviewRepository]
          _          <- reviewRepo.create(rtjvmReview)
          fetchById  <- reviewRepo.getById(1L)
        } yield fetchById

        program.assert { r =>
          r match {
            case Some(review) => {
              review.id == rtjvmReview.id &&
              review.companyId == rtjvmReview.companyId &&
              review.userId == rtjvmReview.userId
            }
            case _ => false
          }
        }
      },
      test("Get all") {
        val program = for {
          reviewRepo <- ZIO.service[ReviewRepository]
          _          <- reviewRepo.create(rtjvmReview)
          allReviews <- reviewRepo.get
        } yield allReviews

        program.assert(reviewList => reviewList.size == 1 && reviewList.head.id == rtjvmReview.id)
      },
      test("Delete a review") {
        val program = for {
          reviewRepo <- ZIO.service[ReviewRepository]
          _          <- reviewRepo.create(rtjvmReview)
          review     <- reviewRepo.delete(1L)
          fetchById  <- reviewRepo.getById(1L)
        } yield fetchById

        program.assert(_.isEmpty)
      }
    ).provide(
      ReviewRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
}
