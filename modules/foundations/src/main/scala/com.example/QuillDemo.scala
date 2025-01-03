package com.example

import zio._
import io.getquill._
import io.getquill.jdbczio.Quill

object QuillDemo extends ZIOAppDefault {

  val program = for {
    repo <- ZIO.service[JobRepository]
    _    <- repo.create(Job(-1, "Software Engineer", "rockthejvm.com", "Rock the JVM"))
    _    <- repo.create(Job(-1, "Instructor", "rockthejvm.com", "Rock the JVM"))
  } yield ()

  def run = program.provide(
    JobRepositoryLive.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase), // quill instance
    Quill.DataSource.fromPrefix("mydbconf")
  ) // reads the config section in application.conf and spins up a data source
}

// repository - each repository will act on a single table
// it will have all methods / operations over jobs
trait JobRepository {
  def create(job: Job): Task[Job]
  def update(id: Long, op: Job => Job): Task[Job]
  def delete(id: Long): Task[Job]
  def getById(id: Long): Task[Option[Job]]
  def get: Task[List[Job]]
}

class JobRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends JobRepository {

  // step 1
  import quill._ // access to quill methods, e.g. run a query

  // step 2 - schemas for create, update...
  // This will tell Quill which case class to convert to / from PG <-> Scala
  // note: we use inline for the macros behind quill to give us the warnings at compile time
  // with the SQL queries that they will run. This is one of the magics of quill library implementation
  inline given schema: SchemaMeta[Job] = schemaMeta[Job]("jobs") // specifies the table name
  inline given insMeta: InsertMeta[Job] =
    insertMeta[Job](_.id) // columns to be excluded (id will be excluded)
  inline given upMeta: UpdateMeta[Job] =
    updateMeta[Job](_.id) // columns to be excluded (id will be excluded)

  override def create(job: Job): Task[Job] =
    run { // run is macro-based
      query[Job]
        .insertValue(lift(job)) // lift is macro-based that will make the job as db representation
        .returning(j => j)
    }

  override def update(id: Long, op: Job => Job): Task[Job] = for {
    current <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing key $id"))
    updated <- run {
      query[Job]
        .filter(_.id == lift(id))
        .updateValue(lift(op(current)))
        .returning(j => j)
    }
  } yield updated

  override def delete(id: Long): Task[Job] =
    run {
      query[Job]
        .filter(_.id == lift(id))
        .delete
        .returning(j => j)
    }

  override def getById(id: Long): Task[Option[Job]] =
    run {
      query[Job]
        .filter(_.id == lift(id)) // select * from jobs where id = ?
    }.map(_.headOption)           // limit 1 in scala instead of in the db

  override def get: Task[List[Job]] = run(query[Job])

}

object JobRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(quill => JobRepositoryLive(quill))
  }
}
