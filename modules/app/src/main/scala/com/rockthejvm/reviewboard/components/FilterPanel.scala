package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{_, given}
import com.raquo.laminar.codecs._
import com.rockthejvm.reviewboard.domain.data.CompanyFilter
import com.rockthejvm.reviewboard.core.ZJS._

/**
  * 1. populate de panel with the right values
  *   a. expose some API that will retrieve the unique values for filtering
  *   b. fetch those values to populate the panel
  **/

/**
  * 2. update the filter panel when they interact with it
  **/

/**
  * 3. when clicking the apply filter button, we should retrieve only those companies
  *   a. make the backend search
  *   b. refetch companies when user clicks the filter
  **/

object FilterPanel {

  case class CheckValueEvent(groupName: String, value: String, checked: Boolean)

  private val GROUP_LOCATIONS  = "Locations"
  private val GROUP_COUNTRIES  = "Countries"
  private val GROUP_INDUSTRIES = "Industries"
  private val GROUP_TAGS       = "Tags"

  private val possibleFilter = Var[CompanyFilter](CompanyFilter.empty)
  private val checkedEvents  = EventBus[CheckValueEvent]()
  private val clicks         = EventBus[Unit]() // clicks on the 'apply' button

  // When I do click on the 'apply' button, this maps to false
  // When I get any new events on 'checkedEvents', this maps to true
  // Dirty is listening for both events. If dirty becomes true, then the 'apply' button should be enabled, and disabled otherwise
  private val dirty = clicks.events.mapTo(false).mergeWith(checkedEvents.events.mapTo(true))

  // This will build the state of the checked boxes by creating a map of (groupName, values) based on the eventbus
  private val state: Signal[CompanyFilter] = checkedEvents.events
    .scanLeft(Map[String, Set[String]]()) { (currentMap, event) =>
      event match {
        case CheckValueEvent(groupName, value, checked) =>
          if (checked) currentMap + (groupName -> (currentMap.getOrElse(groupName, Set()) + value))
          else currentMap + (groupName         -> (currentMap.getOrElse(groupName, Set()) - value))
      }
    }
    .map { checkMap =>
      CompanyFilter(
        locations = checkMap.getOrElse(GROUP_LOCATIONS, Set()).toList,
        countries = checkMap.getOrElse(GROUP_COUNTRIES, Set()).toList,
        industries = checkMap.getOrElse(GROUP_INDUSTRIES, Set()).toList,
        tags = checkMap.getOrElse(GROUP_TAGS, Set()).toList
      )
    }

  // This will generate an event with the value=state at the moment
  // when there's a new value in clicks (so when the button is clicked)
  // This will inform companiesPage to update the list of companies based on the filter
  val triggerFilters: EventStream[CompanyFilter] = clicks.events.withCurrentValueOf(state)

  def apply() =
    div(
      onMountCallback(_ =>
        useBackend(_.companyEndpoints.allFiltersEndpoint(())).map(f => possibleFilter.set(f)).runJs
      ),
      // child.text <-- triggerFilters.map(_.toString()),
      cls    := "accordion accordion-flush",
      idAttr := "accordionFlushExample",
      div(
        cls := "accordion-item",
        h2(
          cls    := "accordion-header",
          idAttr := "flush-headingOne",
          button(
            cls                                         := "accordion-button",
            idAttr                                      := "accordion-search-filter",
            `type`                                      := "button",
            htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
            htmlAttr("data-bs-target", StringAsIsCodec) := "#flush-collapseOne",
            htmlAttr("aria-expanded", StringAsIsCodec)  := "true",
            htmlAttr("aria-controls", StringAsIsCodec)  := "flush-collapseOne",
            div(
              cls := "jvm-recent-companies-accordion-body-heading",
              h3(
                span("Search"),
                " Filters"
              )
            )
          )
        ),
        div(
          cls                                          := "accordion-collapse collapse show",
          idAttr                                       := "flush-collapseOne",
          htmlAttr("aria-labelledby", StringAsIsCodec) := "flush-headingOne",
          htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionFlushExample",
          div(
            cls := "accordion-body p-0",
            renderFilterOptions(GROUP_LOCATIONS, _.locations),
            renderFilterOptions(GROUP_COUNTRIES, _.countries),
            renderFilterOptions(GROUP_INDUSTRIES, _.industries),
            renderFilterOptions(GROUP_TAGS, _.tags),
            renderApplyButton()
          )
        )
      )
    )

  private def renderApplyButton() =
    div(
      cls := "jvm-accordion-search-btn",
      button(
        disabled <-- dirty.toSignal(false).map(v => !v),
        onClick.mapTo(()) --> clicks,
        cls    := "btn btn-primary",
        `type` := "button",
        "Apply Filters"
      )
    )

  def renderFilterOptions(groupName: String, optionsFn: CompanyFilter => List[String]) =
    div(
      cls := "accordion-item",
      h2(
        cls    := "accordion-header",
        idAttr := s"heading$groupName",
        button(
          cls                                         := "accordion-button collapsed",
          `type`                                      := "button",
          htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
          htmlAttr("data-bs-target", StringAsIsCodec) := s"#collapse$groupName",
          htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
          htmlAttr("aria-controls", StringAsIsCodec)  := s"collapse$groupName",
          groupName
        )
      ),
      div(
        cls                                          := "accordion-collapse collapse",
        idAttr                                       := s"collapse$groupName",
        htmlAttr("aria-labelledby", StringAsIsCodec) := "headingOne",
        htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionExample",
        div(
          cls := "accordion-body",
          div(
            cls := "mb-3",
            // stateful Signal, Var
            children <-- possibleFilter.signal.map(filter =>
              optionsFn(filter).map(value => renderCheckbox(groupName, value))
            )
          )
        )
      )
    )

  private def renderCheckbox(groupName: String, value: String) =
    div(
      cls := "form-check",
      label(
        cls   := "form-check-label",
        forId := s"filter-$groupName-$value",
        value
      ),
      input(
        cls    := "form-check-input",
        `type` := "checkbox",
        idAttr := s"filter-$groupName-$value",
        onChange.mapToChecked.map(checked =>
          CheckValueEvent(groupName, value, checked)
        ) --> checkedEvents // emits to checkedEvents
      )
    )

}
