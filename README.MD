## ZIO Rite of passage

Full application based on the [Rock the JVM course "ZIO Rite of passage"](https://rockthejvm.com/courses/zio-rite-of-passage)


## Starting the application

From the root of the project:

1. Spin up a fresh PostgreSQL dockerized database by running `docker-compose up`
2. Run the backend by `sbt "project server" "runMain com.rockthejvm.reviewboard.Application"`
3. Run the frontend by `npm --prefix modules/app/ run start`


## Development

#### Frontend developing:

- Run `sbt` -> `project app` -> `~fastOptJS`  => This will incrementally compile the frontend Scala classes using ScalaJS + Laminar

#### Database management:

- Run `docker exec -it zioriteofpassage-db-1 psql -U docker` after the `docker-compose up` is up and running. This will connect to the DB. After that,
some utility commands are the following:

 - `\c reviewboard;` --> Connects to the reviewboard DB
 - `select * from companies;` --> Selects all companies

 You can check all tables in [the db.sql file](./sql/db.sql)