package com.rockthejvm.reviewboard.core

import org.scalajs.dom
import scala.scalajs._
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.raquo.airstream.state.Var
import scala.scalajs.js.Date

object Session {
  val stateName: String                 = "userState"
  val userState: Var[Option[UserToken]] = Var(Option.empty)

  def isActive = userState.now().nonEmpty

  def setUserState(token: UserToken): Unit = {
    userState.set(Option(token))
    Storage.set(stateName, token)
  }

  def loadUserState(): Unit = {
    // clears any expired token
    Storage
      .get[UserToken](stateName)
      .filter(_.expiration * 1000 <= new Date().getTime())
      .foreach(_ => Storage.remove(stateName))

    userState.set(
      Storage.get[UserToken](stateName)
    )
  }

}
