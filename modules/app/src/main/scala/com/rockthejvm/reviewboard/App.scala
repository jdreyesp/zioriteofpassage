package com.rockthejvm.reviewboard

import com.raquo.laminar.api.L.{_, given}
import org.scalajs.dom
import scala.util.Try
import com.raquo.airstream.ownership.OneTimeOwner
import com.rockthejvm.reviewboard.components.Router
import frontroute.LinkHandler
import com.rockthejvm.reviewboard.components.Header
import com.rockthejvm.reviewboard.core.Session

object App {

  val app = div(
    onMountCallback(_ => Session.loadUserState()),
    Header(),
    Router()
  ).amend(LinkHandler.bind) // for internal links

  def main(args: Array[String]): Unit = {
    val containerNode = dom.document.querySelector("#app")
    render(
      containerNode,
      app
    )
  }
}

object Tutorial {
  val staticContent =
    div(
      // modifiers:
      // - CSS styles
      // - styles
      styleAttr := "color:red", // this is <div style="color:red"/>
      // - onClick
      // - children (that are also modifiers)
      p("Rock the JVM from Laminar !"),
      p("Hello world")
    )

  // Reactive variables: EventStreams, EventBus, Signals, Vars

  // EventStream - produce values of the same type
  // this is observer pattern with steroids - We need to create an owner for the eventStream, an observer to subscribe to the eventstream
  val ticks = EventStream.periodic(1000) // EventStream[Int]
  // subscription - Airstream library
  // ownership (this needs an owner so that it can have observers into it
  val subscription = ticks.addObserver(new Observer[Int] {
    override def onError(err: Throwable): Unit    = ()
    override def onTry(nextValue: Try[Int]): Unit = ()
    override def onNext(nextValue: Int): Unit     = dom.console.log(s"Ticks: $nextValue")
  })(new OneTimeOwner(() => ()))

  // this will kill the stream after 10 seconds
  scala.scalajs.js.timers.setTimeout(10000)(subscription.kill())

  val timeUpdated =
    // In summary, this div has two children: the span, and the second child that is dynamic (that is, its value will be calculated based on the expression)
    div(
      span("Time since loaded: "),
      child <-- ticks.map(number => s"$number seconds")
    )

  // EventBus - like EventStreams, but you can push new elements to the stream
  val clickEvents = EventBus[Int]()
  val clickUpdated = div(
    span("Clicks since loaded: "),
    child <-- clickEvents.events.scanLeft(0)(_ + _).map(number => s"$number seconds"),
    button(
      `type`    := "button",
      styleAttr := "display:block",
      onClick.map(_ => 1) --> clickEvents,
      "Add a click"
    )
  )

  // Signal - similar to EventStreams, bu they have a 'current' value
  // can be inspected for the current state (if Laminar/Airstream knows that it has an owner)
  val countSignal = clickEvents.events.scanLeft(0)(_ + _).observe(new OneTimeOwner(() => ()))
  val queryEvents = EventBus[Unit]()

  val clicksQueried = div(
    span("Clicks since loaded: "),
    child <-- queryEvents.events.map(_ =>
      countSignal.now()
    ), // inspecting the value. This child will ONLY refresh when I click the refresh count button
    button(
      `type`    := "button",
      styleAttr := "display:block",
      onClick.map(_ => 1) --> clickEvents,
      "Add a click"
    ),
    button(
      `type`    := "button",
      styleAttr := "display:block",
      onClick.mapTo(()) --> queryEvents,
      "Refresh count"
    )
  )

  // Var (read and write data) - reactive variable
  val countVar = Var[Int](0)
  val clicksVar = div(
    span("Clicks so far: "),
    // signal listens to changes in this variable
    child <-- countVar.signal.map(_.toString()),
    button(
      `type`    := "button",
      styleAttr := "display:block",
      // These 3 are the same effect
      // onClick --> countVar.updater((current, event) => current + 1),
      // onClick --> countVar.writer.contramap(event => countVar.now() + 1),   // contramap will map the onClick event to the expression described
      onClick --> (_ => countVar.set(countVar.now() + 1)),
      "Add a click"
    )
  )

  /**
   * Summary:
    *         no state    |   with state
    * --------------------+---------------
    * read    EventStream |   Signal
    * --------------------+---------------
    * write   EventBus    |   Var
    */
}
