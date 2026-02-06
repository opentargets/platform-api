package models

import clickhouse.ClickHouseProfile
import esecuele.Column.*
import esecuele.{Query as Q, *}
import models.entities.Configuration.OTSettings
import models.entities.*
import play.api.Logging
import services.ApplicationStart
import slick.basic.DatabaseConfig
import slick.jdbc.{GetResult, SQLActionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success}

class ClickhouseRetriever(config: OTSettings)(implicit
    val dbConfig: DatabaseConfig[ClickHouseProfile],
    val appStart: ApplicationStart
) extends Logging {

  import dbConfig.profile.api._

  val db_name = "clickhouse"

  implicit private def toSQL(q: Q): SQLActionBuilder = sql"""#${q.rep}"""

  var db = dbConfig.db
  val chSettings = config.clickhouse

  def getUniqList[A](of: Seq[String], from: String)(implicit
      rconv: GetResult[A]
  ): Future[Vector[A]] =
    getUniqList[A](of, Column(from))(rconv)

  def getUniqList[A](of: Seq[String], from: Column)(implicit
      rconv: GetResult[A]
  ): Future[Vector[A]] = {
    val s = Select(of.map(column))
    val f = From(from)
    val g = GroupBy(of.map(column))
    val l = Limit(0, 100000)
    val q = Q(s, f, g, l)

    logger.debug(s"getUniqList get distinct $of from $from with query ${q.toString}")
    val qq = q.as[A]

    appStart.DatabaseCallCounter.labelValues(db_name, "getUniqList").inc()

    db.run(qq.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(s"getUniqList an exception was thrown ${ex.getMessage}")
        Vector.empty
    }
  }

  def executeQuery[A, B <: Q](q: B)(implicit rconv: GetResult[A]): Future[Vector[A]] = {
    logger.debug(s"execute query from eselecu Q ${q.toString}")
    val qq = q.as[A]

    appStart.DatabaseCallCounter.labelValues(db_name, "executeQuery").inc()

    db.run(qq.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        lazy val qStr = qq.statements.mkString("\n")
        logger.error(s"executeQuery an exception was thrown ${ex.getMessage} with Query $qStr")
        Vector.empty
    }
  }
}
