package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec
import java.time.Instant
import com.rockthejvm.reviewboard.domain.data.Review

final case class CreateReviewRequest(
    id: Long,
    companyId: Long,
    userId: Long,
    // scores
    management: Int, // 1 - 5
    culture: Int,
    salary: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String,
    created: Instant = Instant.now(),
    updated: Instant = Instant.now()
) {
  def toReview(): Review =
    Review(
      id,
      companyId,
      userId,
      management,
      culture,
      salary,
      benefits,
      wouldRecommend,
      review,
      created,
      updated
    )
}

object CreateReviewRequest {
  given codec: JsonCodec[CreateReviewRequest] = DeriveJsonCodec.gen[CreateReviewRequest]
}
