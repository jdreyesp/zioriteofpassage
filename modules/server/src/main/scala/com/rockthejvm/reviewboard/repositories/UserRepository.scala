package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.User

import zio._
import io.getquill._
import io.getquill.jdbczio.Quill

trait UserRepository {
  def create(user: User): Task[User]
  def update(id: Long, op: User => User): Task[User]
  def getById(id: Long): Task[Option[User]]
  def getByEmail(email: String): Task[Option[User]]
  def delete(id: Long): Task[User]
}

class UserRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends UserRepository {

  import quill._

  inline given schema: SchemaMeta[User]  = schemaMeta[User]("users")
  inline given insMeta: InsertMeta[User] = insertMeta[User](_.id)
  inline given upMeta: UpdateMeta[User]  = updateMeta[User](_.id)

  override def create(user: User): Task[User] =
    run {
      query[User]
        .insertValue(lift(user))
        .returning(r => r)
    }

  override def update(id: Long, op: User => User): Task[User] =
    for {
      current <- getById(id).someOrFail(
        new RuntimeException(s"Cannot update user: id $id not found")
      )
      updated <- run {
        query[User]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(r => r)
      }
    } yield updated

  override def getById(id: Long): Task[Option[User]] =
    run {
      query[User]
        .filter(_.id == lift(id))
    }.map(_.headOption)

  override def getByEmail(email: String): Task[Option[User]] =
    run {
      query[User]
        .filter(_.email == lift(email))
    }.map(_.headOption)

  override def delete(id: Long): Task[User] =
    run {
      query[User]
        .filter(_.id == lift(id))
        .delete
        .returning(r => r)
    }

}

object UserRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(quill => UserRepositoryLive(quill))
  }
}
