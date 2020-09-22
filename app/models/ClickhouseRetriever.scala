package models

import clickhouse.ClickHouseProfile
import esecuele.Column._
import esecuele.{Functions => F, Query => Q, _}
import models.db.{QAOTF, Queryable}
import models.entities.Associations.DBImplicits._
import models.entities.Configuration.{DatasourceSettings, LUTableSettings, OTSettings}
import models.entities.Harmonic._
import models.entities.Network.DBImplicits._
import models.entities._
import play.api.Logger
import slick.basic.DatabaseConfig
import slick.jdbc.{GetResult, SQLActionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ClickhouseRetriever(dbConfig: DatabaseConfig[ClickHouseProfile], config: OTSettings) {
  import dbConfig.profile.api._

  implicit private def toSQL(q: Q): SQLActionBuilder = sql"""#${q.rep}"""
  implicit private def toSQL(q: Queryable): SQLActionBuilder = sql"""#${q.query.rep}"""

  val db = dbConfig.db
  val logger = Logger(this.getClass)
  val chSettings = config.clickhouse
  val datasources = chSettings.harmonic.datasources.toVector
  val diseaseNetworks = chSettings.disease.networks.map(x => x.name -> x).toMap
  val targetNetworks = chSettings.target.networks.map(x => x.name -> x).toMap

  def getDatasourceSettings: Future[Vector[DatasourceSettings]] =
    Future.successful(datasources)

  def getTargetNetworkList: Future[Vector[LUTableSettings]] =
    Future.successful(chSettings.target.networks.toVector)

  def getDiseaseNetworkList: Future[Vector[LUTableSettings]] =
    Future.successful(chSettings.disease.networks.toVector)

  def getNodeNeighbours(netSetttings: LUTableSettings, id: String): Future[Option[NetworkNode]] = {
    val q = Network(netSetttings, id).as[NetworkNode]
    db.run(q.asTry) map {
      case Success(x) => x.headOption
      case Failure(exception) =>
        logger.error(exception.getMessage)
        None
    }
  }

  def getUniqList[A](of: Seq[String], from: String)(implicit rconv: GetResult[A]): Future[Vector[A]] = {
    getUniqList[A](of, Column(from))(rconv)
  }

  def getUniqList[A](of: Seq[String], from: Column)(implicit rconv: GetResult[A]): Future[Vector[A]] = {
    val s = Select(of.map(column))
    val f = From(from)
    val g = GroupBy(of.map(column))
    val l = Limit(0, 100000)
    val q = Q(s, f, g, l)

    logger.debug(s"get distinct $of from $from with query ${q.toString}")
    val qq = q.as[A]

    db.run(qq.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(s"An exception was thrown ${ex.getMessage}")
        Vector.empty
    }
  }

  def executeQuery[A, B <: Q](q: B)(implicit rconv: GetResult[A]) = {
    logger.debug(s"execute query from eselecu Q ${q.toString}")
    val qq = q.as[A]

    db.run(qq.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(s"An exception was thrown ${ex.getMessage}")
        Vector.empty
    }
  }

  def getAssociationsOTF(tableName: String, AId: String, AIDs: Set[String], BIDs: Set[String],
                         BFilter: Option[String],
                         datasourceSettings: Seq[DatasourceSettings],
                         pagination: Pagination) = {
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
      pagination.offset, pagination.size).query.as[Association]

    logger.debug(aotfQ.statements.mkString("\n"))

    db.run(aotfQ.asTry).map {
      case Success(v) => v
      case Failure(ex)  =>
        logger.error(ex.toString)
        logger.error("harmonic associations query failed " +
          s"with query: ${aotfQ.statements.mkString(" ")}")
        Vector.empty
    }
  }
}