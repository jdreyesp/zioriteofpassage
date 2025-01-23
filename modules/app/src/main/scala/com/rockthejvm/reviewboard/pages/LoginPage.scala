package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{_, given}
import org.scalajs.dom
import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.core.ZJS.useBackend
import com.rockthejvm.reviewboard.http.requests.LoginRequest
import com.rockthejvm.reviewboard.core.ZJS._
import frontroute.BrowserNavigation
import zio._
import scala.concurrent.ExecutionContext.Implicits.global
object LoginPage {

  case class State(
      email: String = "",
      password: String = "",
      upstreamError: Option[String] = None,
      showStatus: Boolean = false
  ) {
    val userEmailError: Option[String] =
      Option.when(!email.matches(Constants.emailRegex))("User email is invalid")
    val passwordError: Option[String] =
      Option.when(password.isEmpty)("Password can't be empty")

    val errorList  = List(userEmailError, passwordError)
    val maybeError = errorList.find(_.isDefined).filter(_ => showStatus).flatten
    val hasErrors  = errorList.exists(_.isDefined)
  }
  val stateVar = Var[State](State())

  val submitter = Observer[State] { state =>
    // dom.console.log(s"Current state is: $state")
    // Check state errors -> if so, show them in the error panel
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      // if no errors, trigger the backend call
      useBackend(_.userEndpoints.loginEndpoint(LoginRequest(state.email, state.password)))
        .map { userToken =>
          // if success, set the user token, navigate away
          // TODO set user token
          stateVar.set(State())
          BrowserNavigation.replaceState("/")
        }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showStatus = true, upstreamError = Some(e.getMessage)))
          }
        }
        .runJs
    }

    // if backend gave us an error, show that

  }

  def apply() =
    div(
      cls := "row",
      div(
        cls := "col-md-5 p-0",
        div(
          cls := "logo",
          img(
            src := Constants.logoImage,
            alt := "Rock the JVM"
          )
        )
      ),
      div(
        cls := "col-md-7",
        // right
        div(
          cls := "form-section",
          div(cls := "top-section", h1(span("Log In"))),
          child.text <-- stateVar.signal.map(_.toString),
          children <-- stateVar.signal
            .map(signal => signal.maybeError.orElse(signal.upstreamError).orElse(None))
            .map(_.map(renderError))
            .map(_.toList),
          maybeRenderSuccess(),
          form(
            nameAttr := "signin",
            cls      := "form",
            idAttr   := "form",
            // an input of type text
            renderInput(
              "Email",
              "email-input",
              "text",
              true,
              "Your email",
              (s, e) => s.copy(email = e, showStatus = false, upstreamError = None)
            ),
            // an input of type password
            renderInput(
              "Password",
              "password-input",
              "password",
              true,
              "Your password",
              (s, p) => s.copy(password = p, showStatus = false, upstreamError = None)
            ),
            button(
              `type` := "button",
              "Log In",
              onClick.preventDefault.mapTo(stateVar.now()) --> submitter
            )
          )
        )
      )
    )

  private def renderError(error: String) =
    div(
      cls := "page-status-errors",
      error
    )

  private def maybeRenderSuccess(shouldShow: Boolean = false) =
    if (shouldShow)
      div(
        cls := "page-status-success",
        child.text <-- stateVar.signal.map(_.toString)
      )
    else
      div()

  private def renderInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      plcHolder: String,
      updateFn: (State, String) => State
  ) =
    div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := uid,
            cls   := "form-label",
            if (isRequired) span("*") else span(),
            name
          ),
          input(
            `type`      := kind,
            cls         := "form-control",
            idAttr      := uid,
            placeholder := plcHolder,
            onInput.mapToValue --> stateVar.updater(updateFn)
          )
        )
      )
    )
}
