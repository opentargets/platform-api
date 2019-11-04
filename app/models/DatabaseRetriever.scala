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
import models.entities.Harmonic.Association
import models.entities.Harmonic.Association.DBImplicits._

class DatabaseRetriever(dbConfig: DatabaseConfig[ClickHouseProfile], config: OTSettings) {
  val db = dbConfig.db
  val logger = Logger(this.getClass)
  val chSettings = config.clickhouse
  val datasources = chSettings.harmonic.datasources.toVector
  val diseaseNetworks = chSettings.disease.networks.map(x => x.name -> x).toMap
  val targetNetworks = chSettings.target.networks.map(x => x.name -> x).toMap

  import dbConfig.profile.api._

  def getDatasourceSettings: Future[Vector[DatasourceSettings]] =
    Future.successful(datasources)

  def getTargetNetworkList: Future[Vector[LUTableSettings]] =
    Future.successful(chSettings.target.networks.toVector)

  def getDiseaseNetworkList: Future[Vector[LUTableSettings]] =
    Future.successful(chSettings.disease.networks.toVector)

  /** compute all associations for a disease specified by its `id`
   * and the network expansion method by `expandedBy` field which has to
   * be one of the names you can find in the configuration file in the section
   * ot.clickhouse.disease.networks field name
   * */
  def computeAssociationsDiseaseFixed(id: String, expandedBy: Option[String],
                                      datasourceSettings: Seq[DatasourceSettings],
                                      pagination: Pagination) = {

    // select needs target_id
    val expandedByLUT: Option[LUTableSettings] = expandedBy.flatMap(x => diseaseNetworks.get(x))

    val harmonicQ = Harmonic(config.clickhouse.target.associations.key,
      config.clickhouse.disease.associations.key, id,
      config.clickhouse.disease.associations.name,
      datasourceSettings,
      expandedByLUT,
      pagination)

    val plainQ =
      sql"""#${harmonicQ.rep}""".as[Association]

    logger.debug(harmonicQ.toString)

    db.run(plainQ.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(ex.getMessage)
        Vector.empty
    }
  }

  /** compute all associations for a disease specified by its `id`
   * and the network expansion method by `expandedBy` field which has to
   * be one of the names you can find in the configuration file in the section
   * ot.clickhouse.disease.networks field name
   * */
  def computeAssociationsTargetFixed(id: String, expandedBy: Option[String],
                                      datasourceSettings: Seq[DatasourceSettings],
                                      pagination: Pagination) = {

    // select needs target_id
    val expandedByLUT: Option[LUTableSettings] = expandedBy.flatMap(x => targetNetworks.get(x))

    val harmonicQ = Harmonic(config.clickhouse.disease.associations.key,
      config.clickhouse.target.associations.key, id,
      config.clickhouse.target.associations.name,
      datasourceSettings,
      expandedByLUT,
      pagination)

    val plainQ =
      sql"""#${harmonicQ.rep}""".as[Association]

    logger.debug(harmonicQ.toString)

    db.run(plainQ.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(ex.getMessage)
        Vector.empty
    }
  }
}
