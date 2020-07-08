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
import elesecu.Column._
import elesecu.{Functions => F}
import models.entities.Harmonic._
import models.entities.Associations._
import models.entities.Network.DBImplicits._
import models.entities.Associations.DBImplicits._
import models.entities.Violations.{InputParameterCheckError, PaginationError}
import sangria.validation.Violation
import slick.dbio.DBIOAction
import slick.jdbc.{GetResult, SQLActionBuilder}

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

  def getUniqList[A](of: Seq[String], from: String)(implicit rconv: GetResult[A]) = {
    val s = Select(of.map(column))
    val f = From(column(from))
    val g = GroupBy(of.map(column))
    val q = Q(s, f, g)

    logger.debug(s"get distinct $of from $from with query ${q.toString}")
    val qq = q.as[A]

    db.run(qq.asTry).map {
      case Success(v) => v
      case _ =>
        logger.error("An exception was thrown after quering harmonic and neighbours")
        Vector.empty
    }
  }

  def getAssocationsByDisease(id: String, indirect: Boolean, pagination: Pagination) = {
    /**
     * with if(weight = 0, 1.0, weight) as w,
     * groupArray((datasource_harmonic, w, datasource_id)) as dss,
     * groupUniqArray((datatype_harmonic, w, datatype_id)) as dts,
     * count() as n,
     * range(1, n+1) as idx_v,
     * arrayReverseSort(dss) as dss_r,
     * arraySum((a, b) -> (a.1 * a.2) / pow((b + 1), 2), dss_r, idx_v) as score
     * select target_id as id,
     * any(target_symbol) as symbol,
     * any(target_name) as name,
     * score,
     * dss,
     * dts,
     * n
     * from ot.aotf_indirect_d a
     * left outer join (select v.1 as datasource_id, v.2 as weight
     * from (select array(('europepmc', 0.2),('chembl', 1.0)) as v) array join v) w
     * on (a.datasource_id = w.datasource_id)
     * prewhere disease_id = 'EFO_0000692'
     *  and (ngramSearchCaseInsensitive(target_symbol,'DR')
     *    or ngramSearchCaseInsensitive(target_name, 'dr'))
     * group by disease_id, target_id
     * order by score desc;
     */
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
