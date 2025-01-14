package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec

final case class UserToken(
    email: String,
    token: String,
    expiration: Long // unix time in seconds
) derives JsonCodec
