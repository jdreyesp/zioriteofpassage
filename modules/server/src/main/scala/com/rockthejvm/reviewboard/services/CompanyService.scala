package com.rockthejvm.reviewboard.services

import zio._
import scala.collection.mutable
import com.rockthejvm.reviewboard.domain.data._
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
// BUSINESS LOGIC
// in between the HTTP layer and the DB layer
trait CompanyService {
  def create(
      req: CreateCompanyRequest /* Although this req class belongs to the http package, we reuse it here for simplicity*/
  ): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
}

object CompanyService {
  val dummyLayer = ZLayer.succeed(new CompanyServiceDummy)
}

class CompanyServiceDummy extends CompanyService {

  val db = mutable.Map[Long, Company]()

  override def create(req: CreateCompanyRequest): Task[Company] =
    ZIO.succeed {
      // create an id
      val newId = db.keys.maxOption.getOrElse(0L) + 1
      // create a slug
      // create a company
      val company = req.toCompany(newId)
      // insert the company in the 'database'
      db += (newId -> company)
      // return that company
      company
    }

  override def getAll: Task[List[Company]] = ZIO.succeed(db.values.toList)

  override def getById(id: Long): Task[Option[Company]] = ZIO.succeed(db.get(id))

  override def getBySlug(slug: String): Task[Option[Company]] =
    ZIO.succeed(db.values.find(_.slug == slug))
}
