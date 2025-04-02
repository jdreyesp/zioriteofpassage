package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{_, given}
import org.scalajs.dom
import com.rockthejvm.reviewboard.components.Anchors
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.common.Constants.companyLogoPlaceholder
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import sttp.capabilities._
import sttp.client3._
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.Endpoint
import sttp.model.Uri
import _root_.zio._
import _root_.zio.Unsafe
import com.rockthejvm.reviewboard.core.ZJS
import com.rockthejvm.reviewboard.components.FilterPanel
import com.rockthejvm.reviewboard.core.ZJS.useBackend
import com.rockthejvm.reviewboard.core.ZJS.toEventStream
import ZJS._

object CompaniesPage {

  val firstBatch = EventBus[List[Company]]()

  val companyEvents: EventStream[List[Company]] =
    firstBatch.events
      .mergeWith {
        FilterPanel.triggerFilters.flatMapMerge { newFilter =>
          useBackend(_.companyEndpoints.searchEndpoint(newFilter)).toEventStream
        }
      }

  // val companiesBus = EventBus[List[Company]]()

  // def performBackendCall(): Unit = {
  //   import ZJS._ // imports the extensions
  //   val companiesZIO = useBackend(_.companyEndpoints.getAllEndpoint(()))
  //   companiesZIO.emitTo(companiesBus)
  // }

  def apply() =
    sectionTag(
      onMountCallback(_ =>
        useBackend(client => client.companyEndpoints.getAllEndpoint(()))
          .emitTo(firstBatch)
      ),
      cls := "section-1",
      div(
        cls := "container company-list-hero",
        h1(
          cls := "company-list-title",
          "Rock the JVM Companies Board"
        )
      ),
      div(
        cls := "container",
        div(
          cls := "row jvm-recent-companies-body",
          div(
            cls := "col-lg-4",
            FilterPanel()
          ),
          div(
            cls := "col-lg-8",
            children <-- companyEvents.map(_.map(renderCompany))
          )
        )
      )
    )

  private def renderCompanyPicture(company: Company) =
    img(
      cls := "img-fluid",
      src := company.image.getOrElse(companyLogoPlaceholder),
      alt := company.name
    )

  private def renderDetail(icon: String, value: String) =
    div(
      cls := "company-detail",
      i(cls := s"fa fa-$icon company-detail-icon"),
      p(
        cls := "company-detail-value",
        value
      )
    )

  // city, country
  private def fullLocationString(company: Company) =
    (company.location, company.country) match {
      case (Some(location), Some(country)) => s"$location, $country"
      case (Some(location), None)          => location
      case (None, Some(country))           => country
      case (None, None)                    => "N/A"
    }

  private def renderOverview(company: Company) =
    div(
      cls := "company-summary",
      renderDetail("location-dot", fullLocationString(company)),
      renderDetail("tags", company.tags.mkString(", ")),
      div(
        cls := "company-detail",
        i(cls := s"fa fa-tags company-detail-icon"),
        p(
          cls := "company-detail-value",
          "tag 1, tag 2"
        )
      )
    )

  def renderAction(company: Company) =
    div(
      cls := "jvm-recent-companies-card-btn-apply",
      a(
        href   := "https://todo.com",
        target := "blank",
        button(
          `type` := "button",
          cls    := "btn btn-danger rock-action-btn",
          "Website"
        )
      )
    )

  def renderCompany(company: Company) =
    div(
      cls := "jvm-recent-companies-cards",
      div(
        cls := "jvm-recent-companies-card-img",
        renderCompanyPicture(company)
      ),
      div(
        cls := "jvm-recent-companies-card-contents",
        h5(
          Anchors.renderNavLink(
            company.name,
            s"/company/${company.id}",
            "company-title-link"
          )
        ),
        renderOverview(company)
      ),
      renderAction(company)
    )

}
