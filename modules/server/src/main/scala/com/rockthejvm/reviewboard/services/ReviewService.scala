package com.rockthejvm.reviewboard.services

import zio._
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.repositories.ReviewRepository
import com.rockthejvm.reviewboard.repositories.ReviewRepositoryLive

trait ReviewService {
  def create(req: CreateReviewRequest): Task[Review]
  def getAll: Task[List[Review]]
  def getById(id: Long): Task[Option[Review]]
}

class ReviewServiceLive private (repo: ReviewRepository) extends ReviewService {

  override def create(req: CreateReviewRequest): Task[Review] = repo.create(req.toReview())

  override def getAll: Task[List[Review]] = repo.get

  override def getById(id: Long): Task[Option[Review]] = repo.getById(id)

}

object ReviewServiceLive {
  val layer = ZLayer(ZIO.service[ReviewRepository].map(repo => new ReviewServiceLive(repo)))
}
