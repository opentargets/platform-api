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

  def getAdverseEvents(kv: Map[String, String], pagination: Option[Pagination]):
  Future[Option[AdverseEvents]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val indexName = defaultESSettings.entities
      .find(_.name == "faers").map(_.index).getOrElse("faers")

    val aggs = Seq(
      valueCountAgg("eventCount", "event.keyword")
    )

    import AdverseEvent.JSONImplicits._
    esRetriever.getByIndexedQuery(indexName, kv, pag, fromJsValue[AdverseEvent], aggs, Some("llr")).map {
      case (Seq(), _) => None
      case (seq, agg) =>
        logger.debug(Json.prettyPrint(agg))
        val counts = (agg \ "eventCount" \ "value").as[Long]
        Some(AdverseEvents(counts, seq.head.criticalValue, seq))
    }
  }

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

    import CancerBiomarker.JSONImplicits._
    esRetriever.getByIndexedQuery(cbIndex, kv, pag, fromJsValue[CancerBiomarker], aggs).map {
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

    import ECO.JSONImplicits._
    esRetriever.getByIds(targetIndexName, ids, fromJsValue[ECO])
  }

  def getTargets(ids: Seq[String]): Future[IndexedSeq[Target]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "target").map(_.index).getOrElse("targets")

    import Target.JSONImplicits._
    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Target])
  }

  def getDrugs(ids: Seq[String]): Future[IndexedSeq[Drug]] = {
    val drugIndexName = defaultESSettings.entities
      .find(_.name == "drug").map(_.index).getOrElse("drugs")

    import Drug.JSONImplicits._
    esRetriever.getByIds(drugIndexName, ids, fromJsValue[Drug])
  }

  def getDiseases(ids: Seq[String]): Future[IndexedSeq[Disease]] = {
    val diseaseIndexName = defaultESSettings.entities
      .find(_.name == "disease").map(_.index).getOrElse("diseases")

    import Disease.JSONImplicits._
    esRetriever.getByIds(diseaseIndexName, ids, fromJsValue[Disease])
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
