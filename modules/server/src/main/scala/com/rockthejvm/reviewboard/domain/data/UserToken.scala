package com.rockthejvm.reviewboard.domain.data

final case class UserToken(
    email: String,
    token: String,
    expiration: Long // unix time in seconds
)
