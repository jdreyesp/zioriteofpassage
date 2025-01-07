package com.rockthejvm.reviewboard.config

import zio.config._
import zio.config.magnolia._ /* old scala2 type class library */
import zio._
import zio.config.typesafe.TypesafeConfig
import com.typesafe.config.ConfigFactory

object Configs {

  def makeConfigLayer[C](
      path: String
  )(using desc: Descriptor[C], tag: Tag[C]): ZLayer[Any, Throwable, C] =
    TypesafeConfig.fromTypesafeConfig(
      ZIO.attempt(ConfigFactory.load().getConfig(path)),
      descriptor[C]
    )

}
