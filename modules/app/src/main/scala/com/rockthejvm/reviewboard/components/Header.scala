package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{_, given}
import com.raquo.laminar.codecs._
import org.scalajs.dom

import scala.scalajs.js.annotation._
import scala.scalajs._

import com.rockthejvm.reviewboard.components.Anchors

import com.rockthejvm.reviewboard.common.Constants._

object Header {
  def apply() = div(
    cls := "container-fluid p-0",
    div(
      cls := "jvm-nav",
      div(
        cls := "container",
        navTag(
          cls := "navbar navbar-expand-lg navbar-light JVM-nav",
          renderLogo(),
          div(
            cls := "container",
            // TODO logo
            button(
              cls                                         := "navbar-toggler",
              `type`                                      := "button",
              htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
              htmlAttr("data-bs-target", StringAsIsCodec) := "#navbarNav",
              htmlAttr("aria-controls", StringAsIsCodec)  := "navbarNav",
              htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
              htmlAttr("aria-label", StringAsIsCodec)     := "Toggle navigation",
              span(cls := "navbar-toggler-icon")
            ),
            div(
              cls    := "collapse navbar-collapse",
              idAttr := "navbarNav",
              ul(
                cls := "navbar-nav ms-auto menu align-center expanded text-center SMN_effect-3",
                renderNavLinks()
              )
            )
          )
        )
      )
    )
  )

  private def renderLogo() =
    a(
      href := "/",
      cls  := "navbar-brand",
      img(
        cls := "home-logo",
        src := logoImage,
        alt := "Rock the JVM"
      )
    )

  // list of <li> tags
  // Companies, Log Ing, Sign Up
  private def renderNavLinks() = List(
    renderNavLink("Companies", "/companies"),
    renderNavLink("Log In", "/login"),
    renderNavLink("Sign up", "/signup")
  )

  private def renderNavLink(text: String, location: String) =
    li(
      cls := "nav-item",
      Anchors.renderNavLink(text, location, "nav-link jvm-item")
    )

}
