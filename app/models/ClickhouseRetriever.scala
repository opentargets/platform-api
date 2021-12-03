package models

import clickhouse.ClickHouseProfile
import esecuele.Column._
import esecuele.{Query => Q, _}
import models.db.QAOTF
import models.entities.Associations._
import models.entities.Configuration.{DatasourceSettings, OTSettings}
import models.entities._
import play.api.Logging
import slick.basic.DatabaseConfig
import slick.jdbc.{GetResult, SQLActionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ClickhouseRetriever(dbConfig: DatabaseConfig[ClickHouseProfile], config: OTSettings)
    extends Logging {

  import dbConfig.profile.api._

  implicit private def toSQL(q: Q): SQLActionBuilder = sql"""#${q.rep}"""

  val db = dbConfig.db
  val chSettings = config.clickhouse

  def getUniqList[A](of: Seq[String], from: String)(implicit
                                                    rconv: GetResult[A]
  ): Future[Vector[A]] = {
    getUniqList[A](of, Column(from))(rconv)
  }

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

    db.run(qq.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        val qStr = qq.statements.mkString("\n")
        logger.error(s"executeQuery an exception was thrown ${ex.getMessage} with Query $qStr")
        Vector.empty
    }
  }

  def getAssociationsOTF(
                          tableName: String,
                          AId: String,
                          AIDs: Set[String],
                          BIDs: Set[String],
                          BFilter: Option[String],
                          datasourceSettings: Seq[DatasourceSettings],
                          pagination: Pagination
                        ): Future[Vector[Association]] = {
    val weights = datasourceSettings.map(s => (s.id, s.weight))
    val dontPropagate = datasourceSettings.withFilter(!_.propagate).map(_.id).toSet
    val aotfQ = QAOTF(
      tableName,
      AId,
      AIDs,
      BIDs,
      BFilter,
      None,
      weights,
      dontPropagate,
      pagination.offset,
      pagination.size
    ).query.as[Association]

    logger.debug(aotfQ.statements.mkString("\n"))

    db.run(aotfQ.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(ex.toString)
        logger.error(
          "harmonic associations query failed " +
            s"with query: ${aotfQ.statements.mkString(" ")}"
        )
        Vector.empty
    }
  }
}
