package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{_, given}
import org.scalajs.dom
import frontroute.* // Akka http
import com.rockthejvm.reviewboard.pages._

object Router {
  def apply() =
    mainTag( // main application
      routes(
        div(
          cls := "container-fluid",
          // potential children
          (pathEnd | path("companies")) { // localhost:1234 | localhost:1234/companies
            CompaniesPage()
          },
          path("login") { // localhost:1234/login
            LoginPage()
          },
          path("signup") { // localhost:1234/login
            SignUpPage()
          },
          noneMatched {
            NotFoundPage()
          }
        )
      )
    )
}
