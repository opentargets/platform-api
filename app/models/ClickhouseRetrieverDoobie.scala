package models

import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import cats._
import cats.effect._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import play.api.Logging
import esecuele.Column._
import esecuele.{Query => Q, _}
import models.entities.Configuration.OTSettings
import scala.concurrent.Future
import doobie.util.Read

class ClickhouseRetrieverDoobie(config: OTSettings) extends Logging {

  // private def toSQL(q: Q): Fragment =
  //   // sql"""#${q.rep}"""
  //   fr"""SELECT count(*) FROM ot.intervals WHERE (and(equals(chromosome,'19'),lessOrEquals(start,44908822),greaterOrEquals(end,44908822)))"""

  val chSettings = config.clickhouse

  def executeQuery[A, B <: Q](q: B)(implicit rconv: Read[A]): Future[Vector[A]] = {
    logger.info(s"Executing query with Doobie: ${q.toString}")
    val statement = q.sql

    val qq: ConnectionIO[Vector[A]] = statement.query[A].to[Vector]
    logger.info(s"Query to be executed: ${statement}")

    // Using Doobie to run the query
    val xa = Transactor.fromDriverManager[IO](
      "com.clickhouse.jdbc.ClickHouseDriver",
      "jdbc:clickhouse://127.0.0.1:8123",
      None
    )

    // Running the query
    qq.transact(xa).unsafeToFuture()
  }
}
