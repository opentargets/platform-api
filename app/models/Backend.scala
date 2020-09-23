package models

import clickhouse.ClickHouseProfile
import javax.inject.Inject
import models.Helpers._
import play.api.{Configuration, Environment, Logger}
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.searches.aggs.{CardinalityAggregation, CompositeAggregation, FilterAggregation, NestedAggregation, ReverseNestedAggregation, TermsAggregation, TermsValueSource}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import models.entities.Configuration._
import models.entities.Configuration.JSONImplicits._
import models.db.QAOTF
import models.entities.Associations._
import models.entities.Associations.DBImplicits._
import models.entities._
import models.entities.Aggregations.JSONImplicits._
import models.entities.HealthCheck.JSONImplicits._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.db.NamedDatabase
import esecuele._

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

  lazy val dbRetriever = new ClickhouseRetriever(dbConfigProvider.get[ClickHouseProfile], defaultOTSettings)

  val allSearchableIndices = defaultESSettings.entities
    .withFilter(_.searchIndex.isDefined).map(_.searchIndex.get)

  lazy val esRetriever = new ElasticRetriever(getESClient,
    defaultESSettings.highlightFields,
    allSearchableIndices)

  // we must import the dsl

  import com.sksamuel.elastic4s.ElasticDsl._

  def getRelatedDiseases(kv: Map[String, String], pagination: Option[Pagination]):
  Future[Option[DDRelations]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val indexName = defaultESSettings.entities
      .find(_.name == "disease_relation").map(_.index).getOrElse("disease_relation")

    val aggs = Seq(
      valueCountAgg("relationCount", "B.keyword"),
      maxAgg("maxCountAOrB", "countAOrB")
    )

    import DDRelation.JSONImplicits._
    val excludedFields = List("relatedInfo*")
    esRetriever.getByIndexedQuery(indexName, kv, pag, fromJsValue[DDRelation],
      aggs, ElasticRetriever.sortByDesc("score"), excludedFields).map {
      case (Seq(), _) => None
      case (seq, agg) =>
        logger.debug(Json.prettyPrint(agg))
        val counts = (agg \ "relationCount" \ "value").as[Long]
        val maxCountAOrB = (agg \ "maxCountAOrB" \ "value").as[Long]
        Some(DDRelations(maxCountAOrB, counts, seq))
    }
  }

  def getRelatedTargets(kv: Map[String, String], pagination: Option[Pagination]):
  Future[Option[DDRelations]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val indexName = defaultESSettings.entities
      .find(_.name == "target_relation").map(_.index).getOrElse("target_relation")

    val aggs = Seq(
      valueCountAgg("relationCount", "B.keyword"),
      maxAgg("maxCountAOrB", "countAOrB")
    )

    import DDRelation.JSONImplicits._
    val excludedFields = List("relatedInfo*")
    esRetriever.getByIndexedQuery(indexName, kv, pag, fromJsValue[DDRelation],
      aggs, ElasticRetriever.sortByDesc("score"), excludedFields).map {
      case (Seq(), _) => None
      case (seq, agg) =>
        logger.debug(Json.prettyPrint(agg))
        val counts = (agg \ "relationCount" \ "value").as[Long]
        val maxCountAOrB = (agg \ "maxCountAOrB" \ "value").as[Long]
        Some(DDRelations(maxCountAOrB, counts, seq))
    }
  }

  def getAdverseEvents(kv: Map[String, String], pagination: Option[Pagination]):
  Future[Option[AdverseEvents]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val indexName = defaultESSettings.entities
      .find(_.name == "faers").map(_.index).getOrElse("faers")

    val aggs = Seq(
      valueCountAgg("eventCount", "event.keyword")
    )

    import AdverseEvent.JSONImplicits._
    esRetriever.getByIndexedQuery(indexName, kv, pag, fromJsValue[AdverseEvent], aggs,
      ElasticRetriever.sortByDesc("llr")).map {
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
      cardinalityAgg("uniqueBiomarkers", "id.keyword"),
      valueCountAgg("rowsCount", "id.keyword")
    )

    import CancerBiomarker.JSONImplicits._
    esRetriever.getByIndexedQuery(cbIndex, kv, pag, fromJsValue[CancerBiomarker], aggs).map {
      case (Seq(), _) => None
      case (seq, agg) =>
        logger.debug(Json.prettyPrint(agg))
        val drugs = (agg \ "uniqueDrugs" \ "value").as[Long]
        val diseases = (agg \ "uniqueDiseases" \ "value").as[Long]
        val biomarkers = (agg \ "uniqueBiomarkers" \ "value").as[Long]
        val rowsCount = (agg \ "rowsCount" \ "value").as[Long]
        Some(CancerBiomarkers(drugs, diseases, biomarkers, rowsCount, seq))
    }
  }

  def getKnownDrugs(queryString: String, kv: Map[String, String], sizeLimit: Option[Int],
                    cursor: Seq[String]):
  Future[Option[KnownDrugs]] = {

    val pag = Pagination(0, sizeLimit.getOrElse(Pagination.sizeDefault))
    val sortByField = sort.FieldSort(field = "clinical_trial_phase.raw").desc()
    val cbIndex = defaultESSettings.entities
      .find(_.name == "evidence_drug_direct").map(_.index).getOrElse("evidence_drug_direct")

    val aggs = Seq(
      cardinalityAgg("uniqueTargets", "target.raw"),
      cardinalityAgg("uniqueDiseases", "disease.raw"),
      cardinalityAgg("uniqueDrugs", "drug.raw"),
      //      cardinalityAgg("uniqueClinicalTrials", "list_urls.url.keyword"),
      valueCountAgg("rowsCount", "drug.raw")
    )

    import KnownDrug.JSONImplicits._
    esRetriever.getByFreeQuery(cbIndex, queryString, kv, pag, fromJsValue[KnownDrug],
      aggs, Some(sortByField), Seq("ancestors", "descendants"), cursor).map {
      case (Seq(), _, _) => None
      case (seq, agg, nextCursor) =>
        logger.debug(Json.prettyPrint(agg))
        val drugs = (agg \ "uniqueDrugs" \ "value").as[Long]
        val diseases = (agg \ "uniqueDiseases" \ "value").as[Long]
        val targets = (agg \ "uniqueTargets" \ "value").as[Long]
        //        val clinicalTrials = (agg \ "uniqueClinicalTrials" \ "value").as[Long]
        val rowsCount = (agg \ "rowsCount" \ "value").as[Long]
        Some(KnownDrugs(drugs, diseases, targets, rowsCount, nextCursor, seq))
    }
  }

  def getECOs(ids: Seq[String]): Future[IndexedSeq[ECO]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "eco").map(_.index).getOrElse("ecos")

    import ECO.JSONImplicits._
    esRetriever.getByIds(targetIndexName, ids, fromJsValue[ECO])
  }

  def getMousePhenotypes(ids: Seq[String]): Future[IndexedSeq[MousePhenotypes]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "mp").map(_.index).getOrElse("mp")

    import MousePhenotype.JSONImplicits._
    esRetriever.getByIds(targetIndexName, ids, fromJsValue[MousePhenotypes])
  }

  def getOtarProjects(ids: Seq[String]): Future[IndexedSeq[OtarProjects]] = {
    val otarsIndexName = defaultESSettings.entities
      .find(_.name == "otar_projects").map(_.index).getOrElse("otar_projects")

    import OtarProject.JSONImplicits._
    esRetriever.getByIds(otarsIndexName, ids, fromJsValue[OtarProjects])
  }

  def getExpressions(ids: Seq[String]): Future[IndexedSeq[Expressions]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "expression").map(_.index).getOrElse("expression")

    import Expression.JSONImplicits._
    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Expressions])
  }

  def getReactomeNodes(ids: Seq[String]): Future[IndexedSeq[Reactome]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "reactome").map(_.index).getOrElse("reactome")

    import Reactome.JSONImplicits._
    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Reactome])
  }

  def getTargets(ids: Seq[String]): Future[IndexedSeq[Target]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "target").map(_.index).getOrElse("targets")

    val excludedFields = List("mousePhenotypes*")
    import Target.JSONImplicits._
    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Target],
      excludedFields = excludedFields)
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

  def getAssociationDatasources: Future[Vector[EvidenceSource]] =
    dbRetriever.getUniqList[EvidenceSource](Seq("datasource_id", "datatype_id"), "ot.aotf_direct_d")

  def getAssociationsDiseaseFixed(disease: Disease,
                                  datasources: Option[Seq[DatasourceSettings]],
                                  indirect: Boolean,
                                  targetSet: Set[String],
                                  filter: Option[String],
                                  orderBy: Option[(String, String)],
                                  pagination: Option[Pagination]) = {
    val page = pagination.getOrElse(Pagination.mkDefault)
    val dss = datasources.getOrElse(defaultOTSettings.clickhouse.harmonic.datasources)

    val weights = dss.map(s => (s.id, s.weight))
    val dontPropagate = dss.withFilter(!_.propagate).map(_.id).toSet
    val aotfQ = QAOTF(
      defaultOTSettings.clickhouse.disease.associations.name,
      disease.id,
      _,
      targetSet,
      filter,
      orderBy,
      weights,
      dontPropagate,
      page.offset, page.size)


    logger.debug(s"get disease id ${disease.name}")
    val dIDs = disease.descendants.toSet + disease.id
    val indirectIDs = if (indirect) dIDs else Set.empty[String]
    val q = aotfQ(indirectIDs)
    val simpleQ = q.simpleQuery(0, 100000)
    val fullQ = q.query

    val evidencesIndexName = defaultESSettings.entities
      .find(_.name == "evidences_aotf").map(_.index).getOrElse("evidences_aotf")

    val uniqueTargetsAgg = CardinalityAggregation("uniques", Some("target_id.keyword"),
      precisionThreshold = Some(40000))
    val reverseTargetsAgg = ReverseNestedAggregation("uniques", None, Seq(uniqueTargetsAgg))

    val queryAggs = Seq(
      uniqueTargetsAgg,
      FilterAggregation("dataTypes", boolQuery(),
        subaggs = Seq(
          uniqueTargetsAgg,
          TermsAggregation("aggs", Some("datatype_id.keyword"),
            size = Some(100),
            subaggs = Seq(
              uniqueTargetsAgg,
              TermsAggregation("aggs", Some("datasource_id.keyword"),
                size = Some(100),
                subaggs = Seq(
                  uniqueTargetsAgg
                )
              )
            )
          )
        )
      ),
      FilterAggregation("tractabilitySmallmolecule", boolQuery(),
        subaggs = Seq(
          uniqueTargetsAgg,
          TermsAggregation("aggs", Some("facet_tractability_smallmolecule.keyword"),
            size = Some(100),
            subaggs = Seq(
              uniqueTargetsAgg
            )
          )
        )
      ),
      FilterAggregation("tractabilityAntibody", boolQuery(),
        subaggs = Seq(
          uniqueTargetsAgg,
          TermsAggregation("aggs", Some("facet_tractability_antibody.keyword"),
            size = Some(100),
            subaggs = Seq(
              uniqueTargetsAgg
            )
          )
        )
      ),
      FilterAggregation("pathwayTypes", boolQuery(),
        subaggs = Seq(
          uniqueTargetsAgg,
          NestedAggregation("aggs", path = "facet_reactome",
            subaggs = Seq(
              TermsAggregation("aggs", Some("facet_reactome.l1.keyword"), size = Some(100),
                subaggs = Seq(
                  TermsAggregation("aggs", Some("facet_reactome.l2.keyword"), size = Some(100),
                    subaggs = Seq(reverseTargetsAgg)),
                  reverseTargetsAgg
                )
              ),
              reverseTargetsAgg
            )
          )
        )
      ),
      FilterAggregation("targetClasses", boolQuery(),
        subaggs = Seq(
          uniqueTargetsAgg,
          NestedAggregation("aggs", path = "facet_classes",
            subaggs = Seq(
              TermsAggregation("aggs", Some("facet_classes.l1.keyword"), size = Some(100),
                subaggs = Seq(
                  TermsAggregation("aggs", Some("facet_classes.l2.keyword"), size = Some(100),
                    subaggs = Seq(reverseTargetsAgg)),
                  reverseTargetsAgg
                )
              ),
              reverseTargetsAgg
            )
          )
        )
      )
    )

    val esQ = esRetriever.getAggregationsByQuery(evidencesIndexName,
      boolQuery()
        .withShould(boolQuery()
          .withMust(termsQuery("disease_id.keyword", indirectIDs))
          .withMust(not(termsQuery("datasource_id.keyword", dontPropagate)))
        )
        .withShould(boolQuery()
          .withMust(termQuery("disease_id.keyword", disease.id))
        ),
      queryAggs) map {
      case obj: JsObject =>
        logger.debug(Json.prettyPrint(obj))

        val uniques = (obj \ "uniques" \\ "value").head.as[Long]
        val restAggs = (obj - "uniques").fields map {
          pair =>
            NamedAggregation(pair._1,
              (pair._2 \ "uniques" \\ "value").headOption.map(jv => jv.as[Long]),
              (pair._2 \\ "buckets").head.as[Array[Aggregation]])
        }

        Some(Aggregations(uniques, restAggs))

      case _ => None
    }

    val dbQ = dbRetriever.executeQuery[String, Query](simpleQ) flatMap {
      case tIDs =>
        logger.debug(s"get ${tIDs.size} targets")

        dbRetriever.executeQuery[Association, Query](fullQ) map {
          case assocs => (tIDs.size, assocs)
        }
    }

    dbQ zip esQ map {
      pair => Associations(dss, pair._2, pair._1._1, pair._1._2)
    }
  }

  def getAssociationsTargetFixed(target: Target,
                                 datasources: Option[Seq[DatasourceSettings]],
                                 indirect: Boolean,
                                 diseaseSet: Set[String],
                                 filter: Option[String],
                                 orderBy: Option[(String, String)],
                                 pagination: Option[Pagination]): Future[Associations] = {
    val page = pagination.getOrElse(Pagination.mkDefault)
    val dss = datasources.getOrElse(defaultOTSettings.clickhouse.harmonic.datasources)

    val weights = dss.map(s => (s.id, s.weight))
    val dontPropagate = dss.withFilter(!_.propagate).map(_.id).toSet
    val aotfQ = QAOTF(
      defaultOTSettings.clickhouse.target.associations.name,
      target.id,
      _,
      diseaseSet,
      filter,
      orderBy,
      weights,
      dontPropagate,
      page.offset, page.size)


    logger.debug(s"get target id ${target.approvedSymbol}")
    val tIDs = Set.empty + target.id
    val indirectIDs = if (indirect) tIDs else Set.empty[String]
    val q = aotfQ(indirectIDs)
    val simpleQ = q.simpleQuery(0, 100000)
    val fullQ = q.query

    val evidencesIndexName = defaultESSettings.entities
      .find(_.name == "evidences_aotf").map(_.index).getOrElse("evidences_aotf")

    val uniqueDiseasesAgg = CardinalityAggregation("uniques", Some("disease_id.keyword"),
      precisionThreshold = Some(40000))

    val queryAggs = Seq(
      uniqueDiseasesAgg,
      FilterAggregation("dataTypes", boolQuery(),
        subaggs = Seq(
          uniqueDiseasesAgg,
          TermsAggregation("aggs", Some("datatype_id.keyword"),
            size = Some(100),
            subaggs = Seq(
              uniqueDiseasesAgg,
              TermsAggregation("aggs", Some("datasource_id.keyword"),
                size = Some(100),
                subaggs = Seq(
                  uniqueDiseasesAgg
                )
              )
            )
          )
        )
      ),
      FilterAggregation("therapeuticAreas", boolQuery(),
        subaggs = Seq(
          uniqueDiseasesAgg,
          TermsAggregation("aggs", Some("facet_therapeuticAreas.keyword"),
            size = Some(100),
            subaggs = Seq(
              uniqueDiseasesAgg
            )
          )
        )
      )
    )

    val esQ = esRetriever.getAggregationsByQuery(evidencesIndexName,
      boolQuery()
        .withShould(boolQuery()
          .withMust(termsQuery("target_id.keyword", indirectIDs))
          .withMust(not(termsQuery("datasource_id.keyword", dontPropagate)))
        )
        .withShould(boolQuery()
          .withMust(termQuery("target_id.keyword", target.id))
        ),
      queryAggs) map {
      case obj: JsObject =>
        logger.debug(Json.prettyPrint(obj))

        val uniques = (obj \ "uniques" \\ "value").head.as[Long]
        val restAggs = (obj - "uniques").fields map {
          pair =>
            NamedAggregation(pair._1,
              (pair._2 \ "uniques" \\ "value").headOption.map(jv => jv.as[Long]),
              (pair._2 \\ "buckets").head.as[Array[Aggregation]])
        }

        Some(Aggregations(uniques, restAggs))

      case _ => None
    }

    val dbQ = dbRetriever.executeQuery[String, Query](simpleQ) flatMap {
      case dIDs =>
        logger.debug(s"get ${dIDs.size} diseases")

        dbRetriever.executeQuery[Association, Query](fullQ) map {
          case assocs => (dIDs.size, assocs)
        }
    }

    dbQ zip esQ map {
      pair => Associations(dss, pair._2, pair._1._1, pair._1._2)
    }
  }
}
