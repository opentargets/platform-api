package models

import clickhouse.ClickHouseProfile
import models.entities.Configuration.{DatasourceSettings, LUTableSettings, NetworkSettings, OTSettings, TargetSettings}
import models.entities._
import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._
import slick.basic.DatabaseConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

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
                                      datasourceSettings: Seq[DatasourceSettings]) = {
    expandedBy match {
      case Some(networkName) =>
        diseaseNetworks.get(networkName)
//        harmonicAssociations()
      case None =>
        // assocs without network expansion
    }



  }
//  def getDatasources: Future[Vector[String]] = {
//
//    val plainQ =
//      sql"""
//           |select
//           |  c.name as column_name,
//           |  c.type as column_type
//           |from system.columns c
//           |where c.name like ${colNamePrefix} and c.table = $tableName
//           |order by column_name
//         """.stripMargin.as[DataField]
//
//    //    plainQ.statements.foreach(println)
//
//    db.run(plainQ.asTry).map {
//      case Success(v) => v
//      case Failure(ex) =>
//        logger.error(ex.getMessage)
//        Vector.empty
//    }
//  }
}
