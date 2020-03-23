package models

import clickhouse.ClickHouseProfile
import javax.inject.Inject
import models.Helpers._
import play.api.{Configuration, Environment, Logger}
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.JavaClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}
import models.entities.Configuration._
import models.entities.Configuration.JSONImplicits._
import Entities._
import Entities.JSONImplicits._
import models.entities.Associations._
import models.entities._
import models.entities.HealthCheck.JSONImplicits._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.db.NamedDatabase

class Backend @Inject()(@NamedDatabase("default") protected val dbConfigProvider: DatabaseConfigProvider,
                        config: Configuration,
                        env: Environment) {
  val logger = Logger(this.getClass)

  val defaultOTSettings = loadConfigurationObject[OTSettings]("ot", config)
  val defaultESSettings = defaultOTSettings.elasticsearch

  /** return meta information loaded from ot.meta settings */
  lazy val getMeta: Meta = defaultOTSettings.meta

  def getStatus(isOk: Boolean): HealthCheck =
    if (isOk) HealthCheck(true, "All good!")
    else HealthCheck(false, "Hmm, something wrong is going on here!")

  lazy val getESClient = ElasticClient(JavaClient(
    ElasticProperties(s"http://${defaultESSettings.host}:${defaultESSettings.port}")))

  lazy val dbRetriever = new DatabaseRetriever(dbConfigProvider.get[ClickHouseProfile], defaultOTSettings)

  lazy val esRetriever = new ElasticRetriever(getESClient, defaultESSettings.highlightFields)
  // we must import the dsl
  import com.sksamuel.elastic4s.ElasticDsl._

  def getCancerBiomarkers(kv: Map[String, String], pagination: Option[Pagination]):
    Future[Option[CancerBiomarkers]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val cbIndex = defaultESSettings.entities
      .find(_.name == "cancerBiomarker").map(_.index).getOrElse("cancerbiomarkers")

    val aggs = Seq(
      cardinalityAgg("uniqueDrugs", "drugName.keyword"),
      cardinalityAgg("uniqueDiseases", "disease.keyword"),
      cardinalityAgg("uniqueBiomarkers", "id.keyword")
    )

    esRetriever.getByIndexedQuery(cbIndex, kv, pag, CancerBiomarker.fromJsValue, aggs).map {
      case (Seq(), _) => None
      case (seq, agg) =>
        logger.debug(Json.prettyPrint(agg))
        val drugs = (agg \ "uniqueDrugs" \ "value").as[Long]
        val diseases = (agg \ "uniqueDiseases" \ "value").as[Long]
        val biomarkers = (agg \ "uniqueBiomarkers" \ "value").as[Long]
        Some(CancerBiomarkers(drugs, diseases, biomarkers,seq))
    }
  }

  def getECOs(ids: Seq[String]): Future[IndexedSeq[ECO]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "eco").map(_.index).getOrElse("ecos")

    esRetriever.getByIds(targetIndexName, ids, ECO.fromJsValue)
  }

  def getTargets(ids: Seq[String]): Future[IndexedSeq[Target]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "target").map(_.index).getOrElse("targets")

    esRetriever.getByIds(targetIndexName, ids, Target.fromJsValue)
  }

  def getDrugs(ids: Seq[String]): Future[IndexedSeq[Drug]] = {
    val drugIndexName = defaultESSettings.entities
      .find(_.name == "drug").map(_.index).getOrElse("drugs")

    esRetriever.getByIds(drugIndexName, ids, Drug.fromJsValue)
  }

  def getDiseases(ids: Seq[String]): Future[IndexedSeq[Disease]] = {
    val diseaseIndexName = defaultESSettings.entities
      .find(_.name == "disease").map(_.index).getOrElse("diseases")

    esRetriever.getByIds(diseaseIndexName, ids, Disease.fromJsValue)
  }

  def search(qString: String, pagination: Option[Pagination],
             entityNames: Seq[String]): Future[SearchResults] = {
    val entities = for {
      e <- defaultESSettings.entities
      if (entityNames.contains(e.name) && e.searchIndex.isDefined)
    } yield e

    esRetriever.getSearchResultSet(entities, qString, pagination.getOrElse(Pagination.mkDefault))
  }

  def getAssociationsDiseaseFixed(id: String,
                                  datasources: Option[Seq[DatasourceSettings]],
                                  expansionId: Option[String],
                                  pagination: Option[Pagination]): Future[Associations] = {
    val expandedByLUT: Option[LUTableSettings] =
      expansionId.flatMap(x => dbRetriever.diseaseNetworks.get(x))

    val defaultPagination = Pagination.mkDefault
    val dsV = datasources.getOrElse(defaultOTSettings.clickhouse.harmonic.datasources)
    dbRetriever
      .computeAssociationsDiseaseFixed(id,
        expandedByLUT,
        dsV,
        pagination.getOrElse(defaultPagination))
  }

  def getAssociationsTargetFixed(id: String,
                                 datasources: Option[Seq[DatasourceSettings]],
                                 expansionId: Option[String],
                                 pagination: Option[Pagination]): Future[Associations] = {
    val expandedByLUT: Option[LUTableSettings] =
      expansionId.flatMap(x => dbRetriever.targetNetworks.get(x))

    val defaultPagination = Pagination.mkDefault
    val dsV = datasources.getOrElse(defaultOTSettings.clickhouse.harmonic.datasources)
    dbRetriever
      .computeAssociationsTargetFixed(id,
        expandedByLUT,
        dsV,
        pagination.getOrElse(defaultPagination))
  }
}
