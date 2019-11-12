package models

import clickhouse.ClickHouseProfile
import models.entities.Configuration.{DatasourceSettings, LUTableSettings, OTSettings, TargetSettings}
import models.entities._
import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._
import slick.basic.DatabaseConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import elesecu._
import elesecu.{Query => Q}
import models.entities.Harmonic._
import models.entities.Associations._
import models.entities.Network.DBImplicits._
import models.entities.Associations.DBImplicits._
import models.entities.Violations.{InputParameterCheckError, PaginationError}
import sangria.validation.Violation
import slick.dbio.DBIOAction
import slick.jdbc.SQLActionBuilder

class DatabaseRetriever(dbConfig: DatabaseConfig[ClickHouseProfile], config: OTSettings) {
  import dbConfig.profile.api._

  implicit private def toSQL(q: Q): SQLActionBuilder = sql"""#${q.rep}"""

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

  /** compute all associations for a disease specified by its `id`
   * and the network expansion method by `expandedBy` field which has to
   * be one of the names you can find in the configuration file in the section
   * ot.clickhouse.disease.networks field name
   * */
  def computeAssociationsDiseaseFixed(id: String, expandedBy: Option[LUTableSettings],
                                      datasourceSettings: Seq[DatasourceSettings],
                                      pagination: Pagination) = {

    val neighboursQ = expandedBy
      .map(lut => Network(lut, id).as[NetworkNode].headOption).getOrElse(DBIOAction.successful(None))

    val harmonicQ = Harmonic(config.clickhouse.target.associations.key,
      config.clickhouse.disease.associations.key, id,
      config.clickhouse.disease.associations.name,
      datasourceSettings,
      expandedBy,
      pagination)

    val plainQ = harmonicQ.as[Association]

    logger.debug(harmonicQ.toString)

    if (pagination.hasValidRange()) {
      db.run(plainQ.asTry zip neighboursQ.asTry).map {
        case (Success(v), Success(w)) =>
          Associations(expandedBy, w, datasourceSettings, v)
        case _ =>
          logger.error("An exception was thrown after quering harmonic and neighbours")
          Associations(expandedBy, None, datasourceSettings, Vector.empty)
      }
    } else {
      Future.failed(InputParameterCheckError(
        Vector(PaginationError(pagination.size))))
    }
  }

  /** compute all associations for a disease specified by its `id`
   * and the network expansion method by `expandedBy` field which has to
   * be one of the names you can find in the configuration file in the section
   * ot.clickhouse.disease.networks field name
   * */
  def computeAssociationsTargetFixed(id: String, expandedBy: Option[LUTableSettings],
                                      datasourceSettings: Seq[DatasourceSettings],
                                      pagination: Pagination) = {

    val neighboursQ = expandedBy
      .map(lut => Network(lut, id).as[NetworkNode].headOption).getOrElse(DBIOAction.successful(None))

    val harmonicQ = Harmonic(config.clickhouse.disease.associations.key,
      config.clickhouse.target.associations.key, id,
      config.clickhouse.target.associations.name,
      datasourceSettings,
      expandedBy,
      pagination).as[Association]

    logger.debug(harmonicQ.statements.mkString("\n"))

    db.run(harmonicQ.asTry zip neighboursQ.asTry).map {
      case (Success(v), Success(w)) =>
        Associations(expandedBy, w, datasourceSettings, v)
      case _ =>
        logger.error("An exception was thrown after quering harmonic and neighbours")
        Associations(expandedBy, None, datasourceSettings, Vector.empty)
    }
  }
}
