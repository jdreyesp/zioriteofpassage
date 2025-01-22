package com.rockthejvm.reviewboard.repositories

import zio._
import io.getquill._
import io.getquill.jdbczio.Quill
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.data.CompanyFilter

trait CompanyRepository {
  def create(company: Company): Task[Company]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def get: Task[List[Company]]
  def uniqueAttributes: Task[CompanyFilter]
  def search(filter: CompanyFilter): Task[List[Company]]
}

class CompanyRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends CompanyRepository {

  import quill._

  inline given schema: SchemaMeta[Company]  = schemaMeta[Company]("companies")
  inline given insMeta: InsertMeta[Company] = insertMeta[Company](_.id)
  inline given upMeta: UpdateMeta[Company]  = updateMeta[Company](_.id)

  override def create(company: Company): Task[Company] =
    run {
      query[Company]
        .insertValue(lift(company))
        .returning(r => r)
    }

  override def getById(id: Long): Task[Option[Company]] =
    run {
      query[Company].filter(_.id == lift(id))
    }.map(_.headOption)

  override def getBySlug(slug: String): Task[Option[Company]] =
    run {
      query[Company].filter(_.slug == lift(slug))
    }.map(_.headOption)

  override def get: Task[List[Company]] =
    run(query[Company])

  override def update(id: Long, op: Company => Company): Task[Company] =
    for {
      current <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing id $id"))
      updated <- run {
        query[Company]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(r => r)
      }
    } yield updated

  override def delete(id: Long): Task[Company] =
    run {
      query[Company]
        .filter(_.id == lift(id))
        .delete
        .returning(r => r)
    }

  override def uniqueAttributes: Task[CompanyFilter] =
    for {
      locations <- run(query[Company].map(_.location).distinct).map(list =>
        list.flatMap(o => o.toList)
      )
      countries <- run(query[Company].map(_.country).distinct).map(list =>
        list.flatMap(o => o.toList)
      )
      industries <- run(query[Company].map(_.industry).distinct).map(list =>
        list.flatMap(o => o.toList)
      )
      tags <- run(query[Company].map(_.tags))
        .map(_.flatten.toSet.toList) // toSet is for removing duplicates
    } yield CompanyFilter(locations, countries, industries, tags)

    /*
    select * from companies where
      location in filter.locations OR
      country in filter.countries OR
      industry in filter.industries OR
      tags in (select c1.tags from companies AS c1 where c1.id == company.id)
     */
  override def search(filter: CompanyFilter): Task[List[Company]] =
    if (filter.isEmpty) get
    else {
      run {
        query[Company]
          .filter { company =>
            liftQuery(filter.locations.toSet).contains(company.location) ||
            liftQuery(filter.countries.toSet).contains(company.country) ||
            liftQuery(filter.industries.toSet).contains(company.industry) ||
            sql"${company.tags} && ${lift(filter.tags)}".asCondition // && is interpolation in sql
          }
      }
    }
}

object CompanyRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(quill => CompanyRepositoryLive(quill))
  }
}

object CompanyRepositoryDemo extends ZIOAppDefault {
  val program = for {
    repo <- ZIO.service[CompanyRepository]
    _    <- repo.create(Company(-1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))
  } yield ()

  def run = program.provide(
    CompanyRepositoryLive.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase), // quill instance
    Quill.DataSource.fromPrefix("rockthejvm.db")
  )
}
