package com.rockthejvm.reviewboard.config

final case class EmailServiceConfig(
    host: String,
    port: String,
    user: String,
    pass: String
)
