package com.rockthejvm.reviewboard.common

import scala.scalajs.js.annotation._
import scala.scalajs._

object Constants {
  // This is for converting the image link to JS
  @js.native
  @JSImport("/static/img/fiery-lava 128x128.png", JSImport.Default)
  private[reviewboard] val logoImage: String = js.native

  @js.native
  @JSImport("/static/img/generic_company.png", JSImport.Default)
  private[reviewboard] val companyLogoPlaceholder: String = js.native

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
}
