package models

import clickhouse.ClickHouseProfile
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.aggs._
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import esecuele._

import javax.inject.Inject
import gql.validators.QueryTermsValidator._
import models.Helpers._
import models.db.{QAOTF, QLITAGG, QW2V, SentenceQuery}
import models.entities.Publication._
import models.entities.Aggregations._
import models.entities.Associations._
import models.entities.Configuration._
import models.entities.DiseaseHPOs._
import models.entities.Drug._
import models.entities.MousePhenotypes._
import models.entities.Pharmacogenomics._
import models.entities.SearchFacetsResults._
import models.entities._
import play.api.cache.AsyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.{Configuration, Environment, Logging}
import play.db.NamedDatabase
import sangria.execution.HandledException

import java.time.LocalDate
import scala.collection.immutable.ArraySeq
import scala.concurrent._
import scala.util.{Failure, Success}

class Backend @Inject() (implicit
    ec: ExecutionContext,
    @NamedDatabase("default") protected val dbConfigProvider: DatabaseConfigProvider,
    config: Configuration,
    env: Environment,
    cache: AsyncCacheApi
) extends Logging {

  implicit val defaultOTSettings: OTSettings = loadConfigurationObject[OTSettings]("ot", config)
  implicit val defaultESSettings: ElasticsearchSettings = defaultOTSettings.elasticsearch

  /** return meta information loaded from ot.meta settings */
  lazy val getMeta: Meta = defaultOTSettings.meta
  lazy val getESClient: ElasticClient = ElasticClient(
    JavaClient(ElasticProperties(s"http://${defaultESSettings.host}:${defaultESSettings.port}"))
  )
  val allSearchableIndices: Seq[String] = defaultESSettings.entities
    .withFilter(_.searchIndex.isDefined)
    .map(_.searchIndex.get)

  implicit lazy val dbRetriever: ClickhouseRetriever =
    new ClickhouseRetriever(dbConfigProvider.get[ClickHouseProfile], defaultOTSettings)

  def getStatus(isOk: Boolean): HealthCheck =
    if (isOk) HealthCheck(true, "All good!")
    else HealthCheck(false, "Hmm, something wrong is going on here!")

  implicit lazy val esRetriever: ElasticRetriever =
    new ElasticRetriever(getESClient, defaultESSettings.highlightFields, allSearchableIndices)

  // we must import the dsl

  import com.sksamuel.elastic4s.ElasticDsl._

  def getAdverseEvents(
      id: String,
      pagination: Option[Pagination]
  ): Future[Option[AdverseEvents]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val indexName = getIndexOrDefault("faers")

    val kv = Map("chembl_id.keyword" -> id)

    val aggs = Seq(
      valueCountAgg("eventCount", "chembl_id.keyword")
    )

    esRetriever
      .getByIndexedQueryMust(
        indexName,
        kv,
        pag,
        fromJsValue[AdverseEvent],
        aggs,
        ElasticRetriever.sortByDesc("llr")
      )
      .map {
        case (Seq(), _) =>
          logger.debug(s"No adverse event found for ${kv.toString}")
          None
        case (seq, agg) =>
          logger.trace(Json.prettyPrint(agg))
          val counts = (agg \ "eventCount" \ "value").as[Long]
          Some(AdverseEvents(counts, seq.head.criticalValue, seq))
      }
  }

  def getDiseaseHPOs(id: String, pagination: Option[Pagination]): Future[Option[DiseaseHPOs]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val cbIndex = getIndexOrDefault("disease_hpo")

    val kv = Map("disease.keyword" -> id)

    val aggs = Seq(
      valueCountAgg("rowsCount", "disease.keyword")
    )

    esRetriever.getByIndexedQueryMust(cbIndex, kv, pag, fromJsValue[DiseaseHPO], aggs).map {
      case (Seq(), _) => Some(DiseaseHPOs(0, Seq()))
      case (seq, agg) =>
        logger.trace(Json.prettyPrint(agg))
        val rowsCount = (agg \ "rowsCount" \ "value").as[Long]
        Some(DiseaseHPOs(rowsCount, seq))
    }
  }

  def getGoTerms(ids: Seq[String]): Future[IndexedSeq[GeneOntologyTerm]] = {
    val targetIndexName = getIndexOrDefault("go")

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[GeneOntologyTerm])
  }

  def getTargetEssentiality(ids: Seq[String]): Future[IndexedSeq[TargetEssentiality]] = {
    val targetIndexName = getIndexOrDefault("target_essentiality")

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[TargetEssentiality])
  }

  def getTargetsPrioritisation(id: String): Future[IndexedSeq[JsValue]] = {
    val targetsPrioritisationIndexName = getIndexOrDefault("target_prioritisation")

    esRetriever.getByIds(targetsPrioritisationIndexName, Seq(id), fromJsValue[JsValue])
  }

  def getKeyValuePairsStructure(prioritisation: JsValue) = // Convert to a JsObject
    {
      val myObj: JsObject = prioritisation.as[JsObject]

      // Remove the targetId property
      val updatedObj: JsObject = myObj - "targetId"

      //transform the object in a key value pair array
      val properties = (updatedObj.keys).toSeq
      val keyValuePairs = properties.map { propName =>
        val value = (updatedObj \ propName).get
        Json.obj("key" -> propName, "value" -> value)
      }
      keyValuePairs
    }

  def getTargetsPrioritisationJs(id: String): Future[JsArray] = {
    val result = getTargetsPrioritisation(id)
    val essentialityData = getTargetEssentiality(Seq(id))
    val prioritisationFt = result.map { prioritisationList =>
      val prioritisation = prioritisationList.head

      val arrStructure = getKeyValuePairsStructure(prioritisation)

      val arrStructureWithEssential: Future[Seq[JsObject]] = essentialityData map { case ess =>
        val emptyValue = Json.obj("key" -> "geneEssentiality", "value" -> "")
        if (!ess.isEmpty) {
          val isEssentialOpt = ess.head.geneEssentiality.head.isEssential
          val isEssentialObj = isEssentialOpt match {
            case Some(isEssential) =>
              val essValue = if (isEssential) -1 else 0
              Json.obj("key" -> "geneEssentiality", "value" -> essValue)
            case None => emptyValue
          }
          arrStructure ++ Seq(isEssentialObj)
        } else {
          arrStructure
        }
      }

      arrStructureWithEssential.map(JsArray(_))
    }
    prioritisationFt.flatMap(identity)
  }

  def getKnownDrugs(
      queryString: String,
      kv: Map[String, String],
      sizeLimit: Option[Int],
      cursor: Option[String]
  ): Future[Option[KnownDrugs]] = {

    val pag = Pagination(0, sizeLimit.getOrElse(Pagination.sizeDefault))
    val sortByField = sort.FieldSort(field = "phase").desc()
    val cbIndex = getIndexOrDefault("known_drugs")

    val aggs = Seq(
      cardinalityAgg("uniqueTargets", "targetId.raw"),
      cardinalityAgg("uniqueDiseases", "diseaseId.raw"),
      cardinalityAgg("uniqueDrugs", "drugId.raw"),
      valueCountAgg("rowsCount", "drugId.raw")
    )

    esRetriever
      .getByFreeQuery(
        cbIndex,
        queryString,
        kv,
        pag,
        fromJsValue[KnownDrug],
        aggs,
        Some(sortByField),
        Seq("ancestors", "descendants"),
        cursor
      )
      .map {
        case (Seq(), _, _) => Some(KnownDrugs(0, 0, 0, 0, cursor, Seq()))
        case (seq, agg, nextCursor) =>
          logger.trace(Json.prettyPrint(agg))
          val drugs = (agg \ "uniqueDrugs" \ "value").as[Long]
          val diseases = (agg \ "uniqueDiseases" \ "value").as[Long]
          val targets = (agg \ "uniqueTargets" \ "value").as[Long]
          val rowsCount = (agg \ "rowsCount" \ "value").as[Long]
          Some(KnownDrugs(drugs, diseases, targets, rowsCount, nextCursor, seq))
      }
  }

  // TODO CHECK RESULTS ARE SIZE 0 OR OPTIMISE FIELDS TO BRING BACK

  /** get evidences by multiple parameters */
  def getEvidences(
      datasourceIds: Option[Seq[String]],
      targetIds: Seq[String],
      diseaseIds: Seq[String],
      orderBy: Option[(String, String)],
      sizeLimit: Option[Int],
      cursor: Option[String]
  ): Future[Evidences] = {

    val pag = sizeLimit.getOrElse(Pagination.sizeDefault)
    val sortByField = orderBy.flatMap { p =>
      ElasticRetriever.sortBy(p._1, if (p._2 == "desc") SortOrder.Desc else SortOrder.Asc)
    }

    val cbIndexPrefix = getIndexOrDefault("evidences", Some("evidence_datasource_"))

    val cbIndex = datasourceIds
      .map(_.map(cbIndexPrefix.concat).mkString(","))
      .getOrElse(cbIndexPrefix.concat("*"))

    val kv = Map(
      "targetId.keyword" -> targetIds,
      "diseaseId.keyword" -> diseaseIds
    )

    esRetriever
      .getByMustWithSearch(
        cbIndex,
        kv,
        pag,
        fromJsValue[JsValue],
        Seq.empty,
        sortByField,
        Seq.empty,
        cursor
      )
      .map {
        case (Seq(), n, _) => Evidences.empty(withTotal = n)
        case (seq, n, nextCursor) =>
          Evidences(n, nextCursor, seq)
      }
  }

  def getHPOs(ids: Seq[String]): Future[IndexedSeq[HPO]] = {
    val targetIndexName = getIndexOrDefault("hpo")

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[HPO])
  }

  def getMousePhenotypes(ids: Seq[String]): Future[IndexedSeq[MousePhenotype]] = {
    val indexName = getIndexOrDefault("mouse_phenotypes", Some("mouse_phenotypes"))
    val queryTerm = Map("targetFromSourceId.keyword" -> ids)
    logger.debug(s"Querying mouse phenotypes for: $ids")

    // The entry with the highest number of MP is ENSG00000157404 with 1828. Pagination max size is 5000, so we have plenty
    // of headroom for now.
    esRetriever
      .getByIndexedQueryMust(
        indexName,
        queryTerm,
        Pagination(0, Pagination.sizeMax),
        fromJsValue[MousePhenotype]
      )
      .map(_._1)
  }

  def getPharmacogenomicsByDrug(id: String): Future[IndexedSeq[Pharmacogenomics]] = {
    val queryTerm: Map[String, String] = Map("drugId.keyword" -> id)
    getPharmacogenomics(id, queryTerm)
  }

  def getPharmacogenomicsByTarget(id: String): Future[IndexedSeq[Pharmacogenomics]] = {
    val queryTerm: Map[String, String] = Map("targetFromSourceId.keyword" -> id)
    getPharmacogenomics(id, queryTerm)
  }

  def getPharmacogenomics(id: String,
                          queryTerm: Map[String, String]
  ): Future[IndexedSeq[Pharmacogenomics]] = {
    val indexName = getIndexOrDefault("pharmacogenomics", Some("pharmacogenomics"))
    logger.debug(s"Querying pharmacogenomics for: $id")
    esRetriever
      .getByIndexedQueryMust(
        indexName,
        queryTerm,
        Pagination(0, Pagination.sizeMax),
        fromJsValue[Pharmacogenomics]
      )
      .map(_._1)
  }

  def getOtarProjects(ids: Seq[String]): Future[IndexedSeq[OtarProjects]] = {
    val otarsIndexName = getIndexOrDefault("otar_projects")

    esRetriever.getByIds(otarsIndexName, ids, fromJsValue[OtarProjects])
  }

  def getExpressions(ids: Seq[String]): Future[IndexedSeq[Expressions]] = {
    val targetIndexName = getIndexOrDefault("expression")

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Expressions])
  }

  def getReactomeNodes(ids: Seq[String]): Future[IndexedSeq[Reactome]] = {
    val targetIndexName = getIndexOrDefault("reactome")

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Reactome])
  }

  def getTargets(ids: Seq[String]): Future[IndexedSeq[Target]] = {
    val targetIndexName = getIndexOrDefault("target", Some("targets"))

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Target])
  }

  def getDrugs(ids: Seq[String]): Future[IndexedSeq[Drug]] = {
    logger.debug(s"Querying drugs: $ids")
    val drugIndexName = getIndexOrDefault("drug")
    val queryTerm = Map("id.keyword" -> ids)
    esRetriever
      .getByIndexedQueryShould(drugIndexName, queryTerm, Pagination(0, ids.size), fromJsValue[Drug])
      .map(_._1)
  }

  def getMechanismsOfAction(id: String): Future[MechanismsOfAction] = {

    logger.debug(s"querying ES: getting mechanisms of action for $id")
    val index = getIndexOrDefault("drugMoA")
    val queryTerms = Map("chemblIds.keyword" -> id)
    val mechanismsOfActionRaw: Future[(IndexedSeq[MechanismOfActionRaw], JsValue)] =
      esRetriever.getByIndexedQueryShould(
        index,
        queryTerms,
        Pagination.mkDefault,
        fromJsValue[MechanismOfActionRaw]
      )
    mechanismsOfActionRaw.map(i => Drug.mechanismOfActionRaw2MechanismOfAction(i._1))
  }

  def getIndications(ids: Seq[String]): Future[IndexedSeq[Indications]] = {
    logger.debug(s"querying ES: getting indications for $ids")
    val index = getIndexOrDefault("drugIndications")
    val queryTerm = Map("id.keyword" -> ids)

    esRetriever
      .getByIndexedQueryShould(index, queryTerm, Pagination.mkDefault, fromJsValue[Indications])
      .map(_._1)
  }

  def getDrugWarnings(id: String): Future[IndexedSeq[DrugWarning]] = {
    logger.debug(s"Querying drug warnings for $id")
    val indexName = getIndexOrDefault("drugWarnings")
    val queryTerm = Map("chemblIds.keyword" -> id)
    esRetriever
      .getByIndexedQueryShould(indexName, queryTerm, Pagination.mkDefault, fromJsValue[DrugWarning])
      .map { results =>
        /*
      Group references by warning type and toxicity class to replicate ChEMBL web interface.
      This work around relates to ticket opentargets/platform#1506
         */
        val drugWarnings =
          results._1.foldLeft(Map.empty[(Option[Long]), DrugWarning]) { (dwMap, dw) =>
            if (dwMap.contains((dw.id))) {
              val old = dwMap((dw.id))
              val newDW =
                old.copy(references = Some((old.references ++ dw.references).flatten.toSeq))
              dwMap.updated((dw.id), newDW)
            } else dwMap + ((dw.id) -> dw)
          }
        drugWarnings.values.toIndexedSeq
      }
  }

  def getDiseases(ids: Seq[String]): Future[IndexedSeq[Disease]] = {
    val diseaseIndexName = getIndexOrDefault("disease")
    esRetriever.getByIds(diseaseIndexName, ids, fromJsValue[Disease])
  }

  def mapIds(
      queryTerms: Seq[String],
      entityNames: Seq[String]
  ): Future[MappingResults] = {

    val entities = for {
      e <- defaultESSettings.entities
      if (entityNames.contains(e.name) && e.searchIndex.isDefined)
    } yield e
    withQueryTermsNumberValidation(queryTerms, defaultOTSettings.qValidationLimitNTerms) {
      esRetriever.getTermsResultsMapping(entities, queryTerms)
    }

  }

  def search(
      qString: String,
      pagination: Option[Pagination],
      entityNames: Seq[String]
  ): Future[SearchResults] = {
    val entities = for {
      e <- defaultESSettings.entities
      if (entityNames.contains(e.name) && e.searchIndex.isDefined)
    } yield e
    esRetriever.getSearchResultSet(entities, qString, pagination.getOrElse(Pagination.mkDefault))
  }

  def searchFacets(
      qString: String,
      pagination: Option[Pagination],
      entityNames: Seq[String]
  ): Future[SearchFacetsResults] = {
    val entities = for {
      e <- defaultESSettings.entities
      if (entityNames.contains(e.name) && e.facetSearchIndex.isDefined)
    } yield e
    esRetriever.getSearchFacetsResultSet(entities,
                                         qString,
                                         pagination.getOrElse(Pagination.mkDefault)
    )
  }

  def getAssociationDatasources: Future[Vector[EvidenceSource]] =
    dbRetriever.getUniqList[EvidenceSource](
      Seq("datasource_id", "datatype_id"),
      defaultOTSettings.clickhouse.disease.associations.name
    )

  def getAssociationsDiseaseFixed(
      disease: Disease,
      datasources: Option[Seq[DatasourceSettings]],
      indirect: Boolean,
      facetFilters: Seq[String],
      aggregationFilters: Seq[AggregationFilter],
      targetSet: Set[String],
      filter: Option[String],
      orderBy: Option[(String, String)],
      pagination: Option[Pagination]
  ): Future[Associations] = {
    val page = pagination.getOrElse(Pagination.mkDefault)
    val dss = datasources.getOrElse(defaultOTSettings.clickhouse.harmonic.datasources)

    val weights = dss.map(s => (s.id, s.weight))
    val dontPropagate = dss.withFilter(!_.propagate).map(_.id).toSet
    val aotfQ = QAOTF(
      defaultOTSettings.clickhouse.disease.associations.name,
      disease.id,
      _,
      _,
      filter,
      orderBy,
      weights,
      dontPropagate,
      page.offset,
      page.size
    )

    logger.debug(s"get disease id ${disease.name}")
    val indirectIDs = if (indirect) disease.descendants.toSet + disease.id else Set.empty[String]
    val targetIds = applyFacetFiltersToBIDs("facet_search_target", targetSet, facetFilters)
    val simpleQ = aotfQ(indirectIDs, targetIds).simpleQuery(0, 100000)

    val evidencesIndexName = defaultESSettings.entities
      .find(_.name == "evidences_aotf")
      .map(_.index)
      .getOrElse("evidences_aotf")

    val tractabilityMappings =
      List("SmallMolecule", "Antibody", "Protac", "OtherModalities").map { t =>
        s"tractability$t" -> AggregationMapping(
          s"facet_tractability_${t.toLowerCase}",
          IndexedSeq.empty,
          nested = false
        )
      }.toMap
    val mappings = Map(
      "dataTypes" -> AggregationMapping(
        "datatype_id",
        IndexedSeq("datatype_id", "datasource_id"),
        false
      ),
      "pathwayTypes" -> AggregationMapping("facet_reactome", IndexedSeq("l1", "l2"), true),
      "targetClasses" -> AggregationMapping("facet_classes", IndexedSeq("l1", "l2"), true)
    ) ++ tractabilityMappings

    val queries = ElasticRetriever.aggregationFilterProducer(aggregationFilters, mappings)
    val filtersMap = queries._2

    val uniqueTargetsAgg =
      CardinalityAggregation("uniques", Some("target_id.keyword"), precisionThreshold = Some(40000))
    val reverseTargetsAgg = ReverseNestedAggregation("uniques", None, Seq(uniqueTargetsAgg))

    val queryAggs = Seq(
      FilterAggregation(
        "uniques",
        queries._1,
        subaggs = Seq(
          uniqueTargetsAgg,
          TermsAggregation("ids", field = Some("target_id.keyword"), size = Some(40000))
        )
      ),
      FilterAggregation(
        "dataTypes",
        filtersMap("dataTypes"),
        subaggs = Seq(
          uniqueTargetsAgg,
          TermsAggregation(
            "aggs",
            Some("datatype_id.keyword"),
            size = Some(100),
            subaggs = Seq(
              uniqueTargetsAgg,
              TermsAggregation(
                "aggs",
                Some("datasource_id.keyword"),
                size = Some(100),
                subaggs = Seq(
                  uniqueTargetsAgg
                )
              )
            )
          )
        )
      ),
      FilterAggregation(
        "pathwayTypes",
        filtersMap("pathwayTypes"),
        subaggs = Seq(
          uniqueTargetsAgg,
          NestedAggregation(
            "aggs",
            path = "facet_reactome",
            subaggs = Seq(
              TermsAggregation(
                "aggs",
                Some("facet_reactome.l1.keyword"),
                size = Some(100),
                subaggs = Seq(
                  TermsAggregation(
                    "aggs",
                    Some("facet_reactome.l2.keyword"),
                    size = Some(100),
                    subaggs = Seq(reverseTargetsAgg)
                  ),
                  reverseTargetsAgg
                )
              ),
              reverseTargetsAgg
            )
          )
        )
      ),
      FilterAggregation(
        "targetClasses",
        filtersMap("targetClasses"),
        subaggs = Seq(
          uniqueTargetsAgg,
          NestedAggregation(
            "aggs",
            path = "facet_classes",
            subaggs = Seq(
              TermsAggregation(
                "aggs",
                Some("facet_classes.l1.keyword"),
                size = Some(100),
                subaggs = Seq(
                  TermsAggregation(
                    "aggs",
                    Some("facet_classes.l2.keyword"),
                    size = Some(100),
                    subaggs = Seq(reverseTargetsAgg)
                  ),
                  reverseTargetsAgg
                )
              ),
              reverseTargetsAgg
            )
          )
        )
      )
    ) ++ tractabilityMappings.map { kv =>
      FilterAggregation(
        kv._1,
        ElasticRetriever.aggregationFilterProducer(aggregationFilters, Map(kv))._1,
        subaggs = Seq(
          uniqueTargetsAgg,
          TermsAggregation(
            "aggs",
            Some(s"${kv._2.key}.keyword"),
            size = Some(100),
            subaggs = Seq(
              uniqueTargetsAgg
            )
          )
        )
      )
    }

    val esQ = esRetriever.getAggregationsByQuery(
      evidencesIndexName,
      boolQuery()
        .withShould(
          boolQuery()
            .withMust(termsQuery("disease_id.keyword", indirectIDs))
            .withMust(not(termsQuery("datasource_id.keyword", dontPropagate)))
        )
        .withShould(
          boolQuery()
            .withMust(termQuery("disease_id.keyword", disease.id))
        ),
      queryAggs
    ) map {
      case obj: JsObject =>
        logger.trace(Json.prettyPrint(obj))

        val ids = (obj \ "uniques" \ "ids" \ "buckets" \\ "key").map(_.as[String]).toSet
        val uniques = (obj \ "uniques" \\ "value").head.as[Long]
        val restAggs: Seq[NamedAggregation] = ((obj - "uniques").fields map { pair =>
          NamedAggregation(
            pair._1,
            (pair._2 \ "uniques" \\ "value").headOption.map(jv => jv.as[Long]),
            ArraySeq.unsafeWrapArray((pair._2 \\ "buckets").head.as[Array[entities.Aggregation]])
          )
        }).to(Seq)

        Some((Aggregations(uniques, restAggs), ids))

      case _ => None
    }

    // TODO use option to enable or disable the computation of each of the sides
    (dbRetriever.executeQuery[String, Query](simpleQ) zip esQ) flatMap { case (tIDs, esR) =>
      val tids = esR.map(_._2 intersect tIDs.toSet).getOrElse(tIDs.toSet)
      val fullQ = aotfQ(indirectIDs, tids).query

      logger.debug(
        s"disease fixed get simpleQ n ${tIDs.size} " +
          s"agg n ${esR.map(_._2.size).getOrElse(-1)} " +
          s"inter n ${tids.size}"
      )

      if (tids.nonEmpty) {
        dbRetriever.executeQuery[Association, Query](fullQ) map { case assocs =>
          Associations(dss, esR.map(_._1), tids.size, assocs)
        }
      } else {
        Future.successful(Associations(dss, esR.map(_._1), tids.size, Vector.empty))
      }
    }
  }

  def getAssociationsTargetFixed(
      target: Target,
      datasources: Option[Seq[DatasourceSettings]],
      indirect: Boolean,
      facetFilters: Seq[String],
      aggregationFilters: Seq[AggregationFilter],
      diseaseSet: Set[String],
      filter: Option[String],
      orderBy: Option[(String, String)],
      pagination: Option[Pagination]
  ): Future[Associations] = {
    val page = pagination.getOrElse(Pagination.mkDefault)
    val dss = datasources.getOrElse(defaultOTSettings.clickhouse.harmonic.datasources)

    val weights = dss.map(s => (s.id, s.weight))
    val dontPropagate = dss.withFilter(!_.propagate).map(_.id).toSet
    val aotfQ = QAOTF(
      defaultOTSettings.clickhouse.target.associations.name,
      target.id,
      _,
      _,
      filter,
      orderBy,
      weights,
      dontPropagate,
      page.offset,
      page.size
    )

    logger.debug(s"get target id ${target.approvedSymbol} ACTUALLY DISABLED!")
    val indirectIDs = if (indirect) {
      val interactions =
        Interactions.find(target.id, None, pagination = Some(Pagination(0, 10000))) map {
          case Some(ints) =>
            ints.rows
              .flatMap(int => (int \ ("targetB")).asOpt[String].filter(_.startsWith("ENSG")))
              .toSet + target.id
          case None => Set.empty + target.id
        }

      interactions.await

    } else Set.empty[String]

    val diseaseIds =
      applyFacetFiltersToBIDs("facet_search_disease", diseaseSet, facetFilters)
    val simpleQ = aotfQ(indirectIDs, diseaseIds).simpleQuery(0, 100000)

    val evidencesIndexName = defaultESSettings.entities
      .find(_.name == "evidences_aotf")
      .map(_.index)
      .getOrElse("evidences_aotf")

    val mappings = Map(
      "dataTypes" -> AggregationMapping(
        "datatype_id",
        IndexedSeq("datatype_id", "datasource_id"),
        false
      ),
      "therapeuticAreas" -> AggregationMapping("facet_therapeuticAreas", IndexedSeq.empty, false)
    )

    val queries = ElasticRetriever.aggregationFilterProducer(aggregationFilters, mappings)
    val filtersMap = queries._2

    val uniqueDiseasesAgg = CardinalityAggregation(
      "uniques",
      Some("disease_id.keyword"),
      precisionThreshold = Some(40000)
    )

    val queryAggs = Seq(
      FilterAggregation(
        "uniques",
        queries._1,
        subaggs = Seq(
          uniqueDiseasesAgg,
          TermsAggregation("ids", field = Some("disease_id.keyword"), size = Some(40000))
        )
      ),
      FilterAggregation(
        "dataTypes",
        filtersMap("dataTypes"),
        subaggs = Seq(
          uniqueDiseasesAgg,
          TermsAggregation(
            "aggs",
            Some("datatype_id.keyword"),
            size = Some(100),
            subaggs = Seq(
              uniqueDiseasesAgg,
              TermsAggregation(
                "aggs",
                Some("datasource_id.keyword"),
                size = Some(100),
                subaggs = Seq(
                  uniqueDiseasesAgg
                )
              )
            )
          )
        )
      ),
      FilterAggregation(
        "therapeuticAreas",
        filtersMap("therapeuticAreas"),
        subaggs = Seq(
          uniqueDiseasesAgg,
          TermsAggregation(
            "aggs",
            Some("facet_therapeuticAreas.keyword"),
            size = Some(100),
            subaggs = Seq(
              uniqueDiseasesAgg
            )
          )
        )
      )
    )

    val esQ = esRetriever.getAggregationsByQuery(
      evidencesIndexName,
      boolQuery()
        .withShould(
          boolQuery()
            .withMust(termsQuery("target_id.keyword", indirectIDs))
            .withMust(not(termsQuery("datasource_id.keyword", dontPropagate)))
        )
        .withShould(
          boolQuery()
            .withMust(termQuery("target_id.keyword", target.id))
        ),
      queryAggs
    ) map {
      case obj: JsObject =>
        logger.trace(Json.prettyPrint(obj))

        val ids = (obj \ "uniques" \ "ids" \ "buckets" \\ "key").map(_.as[String]).toSet
        val uniques = (obj \ "uniques" \\ "value").head.as[Long]
        val restAggs = (obj - "uniques").fields map { pair =>
          NamedAggregation(
            pair._1,
            (pair._2 \ "uniques" \\ "value").headOption.map(jv => jv.as[Long]),
            ArraySeq.unsafeWrapArray((pair._2 \\ "buckets").head.as[Array[entities.Aggregation]])
          )
        }

        Some((Aggregations(uniques, restAggs.to(Seq)), ids))

      case _ => None
    }

    (dbRetriever.executeQuery[String, Query](simpleQ) zip esQ) flatMap { case (dIDs, esR) =>
      val dids = esR.map(_._2 intersect dIDs.toSet).getOrElse(dIDs.toSet)
      val fullQ = aotfQ(indirectIDs, dids).query

      logger.debug(
        s"target fixed get simpleQ n ${dIDs.size} " +
          s"agg n ${esR.map(_._2.size).getOrElse(-1)} " +
          s"inter n ${dids.size}"
      )

      if (dids.nonEmpty) {
        dbRetriever.executeQuery[Association, Query](fullQ) map { case assocs =>
          Associations(dss, esR.map(_._1), dids.size, assocs)
        }
      } else {
        Future.successful(Associations(dss, esR.map(_._1), dids.size, Vector.empty))
      }
    }
  }

  def getSimilarW2VEntities(
      label: String,
      labels: Set[String],
      categories: List[String],
      threshold: Double,
      size: Int
  ): Future[Vector[Similarity]] = {
    val table = defaultOTSettings.clickhouse.similarities
    logger.info(s"query similarities in table ${table.name}")

    val jointLabels = labels + label
    val simQ = QW2V(table.name, categories, jointLabels, threshold, size)
    dbRetriever.executeQuery[Long, Query](simQ.existsLabel(label)).flatMap {
      case Vector(1) => dbRetriever.executeQuery[Similarity, Query](simQ.query)
      case _ =>
        logger.info(
          s"This case where the label asked ${label} to the model Word2Vec does not exist" +
            s" is ok but nice to capture though"
        )
        Future.successful(Vector.empty)
    }
  }

  def getLiteratureSentences(
      pmid: String
  ): Future[Map[String, Vector[Sentence]]] = {
    val table = defaultOTSettings.clickhouse.sentences
    logger.debug(s"Query sentences for $pmid from table ${table.name}")
    val sentenceQuery = SentenceQuery(pmid, table.name)
    val results = dbRetriever.executeQuery[Sentence, Query](sentenceQuery.query)
    results.map(vs => vs.groupMap(_.section)(identity))
  }

  def getLiteratureOcurrences(ids: Set[String], cursor: Option[String]): Future[Publications] = {
    import Pagination._

    getLiterature(ids, Option.empty, Option.empty, Option.empty, Option.empty, cursor)
  }

  def getLiteratureOcurrences(ids: Set[String],
                              startYear: Option[Int],
                              startMonth: Option[Int],
                              endYear: Option[Int],
                              endMonth: Option[Int],
                              cursor: Option[String]
  ): Future[Publications] = {
    import Pagination._

    getLiterature(ids, startYear, startMonth, endYear, endMonth, cursor)
  }

  private def getLiterature(ids: Set[String],
                            startYear: Option[Int],
                            startMonth: Option[Int],
                            endYear: Option[Int],
                            endMonth: Option[Int],
                            cursor: Option[String]
  ): Future[Publications] = {
    val table = defaultOTSettings.clickhouse.literature
    val indexTable = defaultOTSettings.clickhouse.literatureIndex
    logger.info(s"query literature ocurrences in table ${table.name}")

    val pag = Helpers.Cursor.to(cursor).flatMap(_.asOpt[Pagination]).getOrElse(Pagination.mkDefault)

    val filterDate = (startYear, endYear) match {
      case (Some(strYear), Some(ndYear)) =>
        Some(strYear, startMonth.getOrElse(1), ndYear, endMonth.getOrElse(12))
      case _ => Option.empty
    }

    val simQ = QLITAGG(table.name, indexTable.name, ids, pag.size, pag.offset, filterDate)

    def runQuery(year: Int, total: Long) =
      dbRetriever.executeQuery[Publication, Query](simQ.query).map { v =>
        val pubs = v
          .map(pub => Json.toJson(pub))
        val nCursor = if (v.size < pag.size) {
          None
        } else {
          val npag = pag.next
          Helpers.Cursor.from(Some(Json.toJson(npag)))
        }

        val result = dbRetriever.executeQuery[Int, Query](simQ.filteredTotalQ).map { v2 =>
          Publications(total, year, nCursor, pubs, v2.head)
        }

        result.await
      }

    dbRetriever.executeQuery[Long, Query](simQ.total).flatMap {
      case Vector(total) if total > 0 =>
        logger.debug(s"total number of publication occurrences $total")
        dbRetriever.executeQuery[Int, Query](simQ.minDate).flatMap {
          case Vector(year) =>
            runQuery(year, total)
          case _ =>
            logger.info(s"Cannot find the earliest year for the publications.")
            runQuery(1900, total)
        }

      case _ =>
        logger.info(s"there is no publications with this set of ids $ids")
        Future.successful(Publications.empty())
    }
  }

  def filterLiteratureByDate(pub: Publication, dateAndComparator: (Int, Int, Int, Int)): Boolean = {
    // if no year is sent no filter is applied

    def compareDates(pubDate: LocalDate, reqStartDate: LocalDate, reqEndDate: LocalDate): Boolean =
      pubDate.compareTo(reqStartDate) >= 0 && pubDate.compareTo(reqEndDate) <= 0

    val pubDate = LocalDate.of(pub.year, pub.month, 1)
    val reqStartDate = LocalDate.of(dateAndComparator._1, dateAndComparator._2, 1)
    val reqEndDate = LocalDate.of(dateAndComparator._3, dateAndComparator._4, 1)

    compareDates(pubDate, reqStartDate, reqEndDate)

  }

  /** @param index
    * key of index (name field) in application.conf
    * @param default
    * fallback index name
    * @return
    * elasticsearch index name resolved from application.conf or default.
    */
  private def getIndexOrDefault(index: String, default: Option[String] = None): String =
    defaultESSettings.entities
      .find(_.name == index)
      .map(_.index)
      .getOrElse(default.getOrElse(index))

  /** Get the entity ids for a given set of facet filters.
    * @return
    * An indexed sequence of entity id sets.
    */
  private def resolveEntityIdsFromFacets(facetFilters: Seq[String],
                                         index: String
  ): IndexedSeq[Seq[String]] = {
    val facets =
      esRetriever.getByIds(getIndexOrDefault(index), facetFilters, fromJsValue[Facet])
    facets.await.map(_.entityIds.getOrElse(Seq.empty))
  }

  /** Reduce a set of BIDs with the BIDs derived from the facets.
    * If the set of BIDs is empty, the BIDs are derived from the facets.
    * If the set of facets is empty, the BIDs are returned as is.
    * If both the set of BIDs and the set of facets are not empty, the BIDs are intersected with the BIDs derived from the facets.
    * If the intersection is empty, a Set of "" is returned to ensure that no ids are returned.
    *
    * @param index
    * @param bIDs
    * @param facetFilters
    * @return
    */
  private def applyFacetFiltersToBIDs(index: String,
                                      bIDs: Set[String],
                                      facetFilters: Seq[String]
  ): Set[String] =
    if (facetFilters.isEmpty) {
      bIDs
    } else {
      val entityIdsFromFacets: IndexedSeq[Seq[String]] =
        resolveEntityIdsFromFacets(facetFilters, index)
      val entityIdsFromFacetsIntersect: Set[String] =
        if (entityIdsFromFacets.isEmpty) Set.empty
        else entityIdsFromFacets.map(_.toSet).reduce(_ intersect _)
      if (bIDs.isEmpty) emptySetToSetOfEmptyString(entityIdsFromFacetsIntersect)
      else emptySetToSetOfEmptyString(bIDs.intersect(entityIdsFromFacetsIntersect))
    }
}
