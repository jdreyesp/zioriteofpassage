package com.rockthejvm.reviewboard.services

import zio.test.ZIOSpecDefault
import zio._
import zio.test.Spec
import zio.test.TestEnvironment
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.syntax._
import com.rockthejvm.reviewboard.repositories.CompanyRepository
import com.rockthejvm.reviewboard.domain.data.Company
object CompanyServiceSpec extends ZIOSpecDefault {

  // This exposes the CompanyService through the ZIO dependency system (via environment)
  // This is a powerful alternative of companion object's factory methods when we don't want
  // to expose the creation of the object outside its file.
  val service        = ZIO.serviceWithZIO[CompanyService]
  val companyRequest = CreateCompanyRequest("Rock the JVM", "rockthejvm.com")

  // We give certain functionality to the repo layer so that we can test the whole suite.
  // Personally, I don't like this, I'd prefer to use an immutable state of the DB in each
  // test scenario.
  val stubRepoLayer = ZLayer.succeed(
    new CompanyRepository {

      val db = collection.mutable.Map[Long, Company]()

      override def create(company: Company): Task[Company] =
        ZIO.succeed {
          val nextId     = db.keys.maxOption.getOrElse(0L) + 1
          val newCompany = company.copy(id = nextId)
          db += (nextId -> newCompany)
          newCompany
        }

      override def update(id: Long, op: Company => Company): Task[Company] =
        ZIO.attempt {
          val company = db(id)
          db += (id -> op(company))
          company
        }

      override def delete(id: Long): Task[Company] =
        ZIO.attempt {
          val company = db(id)
          db -= id
          company
        }

      override def getById(id: Long): Task[Option[Company]] =
        ZIO.succeed(db.get(id))

      override def getBySlug(slug: String): Task[Option[Company]] =
        ZIO.succeed(db.values.find(_.slug == slug))

      override def get: Task[List[Company]] =
        ZIO.succeed(db.values.toList)
    }
  )
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyServiceSpec")(
      test("create") {
        val companyZIO = service(_.create(companyRequest))
        companyZIO.assert { company =>
          company.name == "Rock the JVM" &&
          company.url == "rockthejvm.com" &&
          company.slug == "rock-the-jvm"
        }
      },
      test("get by id") {
        // create a company
        // fetch a company by its id
        val program = for {
          company    <- service(_.create(companyRequest))
          companyOpt <- service(_.getById(company.id))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyRes)) =>
            company.name == "Rock the JVM" &&
            company.url == "rockthejvm.com" &&
            company.slug == "rock-the-jvm" &&
            company == companyRes
          case _ => false
        }
      },
      test("get by slug") {
        val program = for {
          company    <- service(_.create(companyRequest))
          companyOpt <- service(_.getBySlug(company.slug))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyRes)) =>
            company.name == "Rock the JVM" &&
            company.url == "rockthejvm.com" &&
            company.slug == "rock-the-jvm" &&
            company == companyRes
          case _ => false
        }
      },
      test("get all") {
        val program = for {
          company   <- service(_.create(companyRequest))
          company2  <- service(_.create(CreateCompanyRequest("Google", "google.com")))
          companies <- service(_.getAll)
        } yield (company, company2, companies)

        program.assert {
          case (company, company2, companies) =>
            companies == List(company, company2)
          case null => false
        }
      }
    ).provide(CompanyServiceLive.layer, stubRepoLayer)
}
