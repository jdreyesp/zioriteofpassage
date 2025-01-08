package com.rockthejvm.reviewboard.repositories

import org.testcontainers.containers.PostgreSQLContainer
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource
import zio.ZLayer
import zio.ZIO

trait Repositories {

  val initScript: String

  // test containers (suite of libraries in the Java world (and of course it works in Scala too))
  // that allows us to spin up lightweight docker containers for tests.
  // spawn a Postgres instance on Docker just for the test
  private def createContainer() = {
    val container: PostgreSQLContainer[Nothing] =
      PostgreSQLContainer("postgres").withInitScript(initScript)

    container.start()
    container
  }

  // create a DataSource to connect to the Postgres instance
  private def createDataSource(container: PostgreSQLContainer[Nothing]): DataSource = {
    val dataSource = new PGSimpleDataSource()
    dataSource.setURL(container.getJdbcUrl())
    dataSource.setUser(container.getUsername())
    dataSource.setPassword(container.getPassword())
    dataSource
  }

  // use the DataSource (as a ZLayer) to build the Quill instance
  val dataSourceLayer = ZLayer {
    for {
      container <- ZIO.acquireRelease(ZIO.attempt(createContainer()))(container =>
        ZIO.attempt(container.stop()).ignoreLogged
      )
      dataSource <- ZIO.attempt(createDataSource(container))
    } yield dataSource
  }
}
