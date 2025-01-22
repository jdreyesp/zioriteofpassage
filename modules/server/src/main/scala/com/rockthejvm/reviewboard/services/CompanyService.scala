package com.rockthejvm.reviewboard.services

import zio._
import scala.collection.mutable
import com.rockthejvm.reviewboard.domain.data._
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.repositories.CompanyRepository
import com.rockthejvm.reviewboard.repositories.CompanyRepositoryLive
// BUSINESS LOGIC
// in between the HTTP layer and the DB layer
trait CompanyService {
  def create(
      req: CreateCompanyRequest /* Although this req class belongs to the http package, we reuse it here for simplicity*/
  ): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def allFilters: Task[
    CompanyFilter
  ] /* Will show all possible filtering options based on our current list of companies in the DB */
  def search(filter: CompanyFilter): Task[List[Company]]
}

class CompanyServiceLive private (repo: CompanyRepository) extends CompanyService {

  override def allFilters: Task[CompanyFilter] = repo.uniqueAttributes

  override def create(req: CreateCompanyRequest): Task[Company] = repo.create(req.toCompany(-1))

  override def getAll: Task[List[Company]] = repo.get

  override def getById(id: Long): Task[Option[Company]] = repo.getById(id)

  override def getBySlug(slug: String): Task[Option[Company]] = repo.getBySlug(slug)

  override def search(filter: CompanyFilter): Task[List[Company]] = repo.search(filter)
}

object CompanyServiceLive {
  val layer = ZLayer {
    for {
      repo <- ZIO.service[CompanyRepository]
    } yield new CompanyServiceLive(repo)
  }
}
