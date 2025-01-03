package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

final case class Company(
    id: Long,
    // snake-case url friendly name built from the `name` field
    // (e.g. for company name 'Rock The JVM', rockthejvm.com/company/rock-the-jvm) <-
    slug: String,
    name: String,
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = List()
)

object Company {
  given codec: JsonCodec[Company] = DeriveJsonCodec.gen[Company]

  def makeSlug(name: String): String =
    name
      .replaceAll(" +", " ") // replace multiple spaces into a single one
      .split(" ")
      .map(_.toLowerCase())
      .mkString("-")
}
