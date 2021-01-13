package models

import clickhouse.ClickHouseProfile
import javax.inject.Inject
import models.Helpers._
import play.api.{Configuration, Environment, Logging}
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.searches.aggs._
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder

import scala.concurrent._
import models.db.QAOTF
import models.entities._
import models.entities.Drug._
import models.entities.Configuration._
import models.entities.CancerBiomarkers._
import models.entities.Aggregations._
import models.entities.Associations._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.db.NamedDatabase
import esecuele._

class Backend @Inject()(
    implicit ec: ExecutionContext,
    @NamedDatabase("default") protected val dbConfigProvider: DatabaseConfigProvider,
    config: Configuration,
    env: Environment)
    extends Logging {

  implicit val defaultOTSettings = loadConfigurationObject[OTSettings]("ot", config)
  implicit val defaultESSettings = defaultOTSettings.elasticsearch

  /** return meta information loaded from ot.meta settings */
  lazy val getMeta: Meta = defaultOTSettings.meta

  def getStatus(isOk: Boolean): HealthCheck =
    if (isOk) HealthCheck(true, "All good!")
    else HealthCheck(false, "Hmm, something wrong is going on here!")

  lazy val getESClient = ElasticClient(
    JavaClient(ElasticProperties(s"http://${defaultESSettings.host}:${defaultESSettings.port}")))

  implicit lazy val dbRetriever =
    new ClickhouseRetriever(dbConfigProvider.get[ClickHouseProfile], defaultOTSettings)

  val allSearchableIndices = defaultESSettings.entities
    .withFilter(_.searchIndex.isDefined)
    .map(_.searchIndex.get)

  implicit lazy val esRetriever =
    new ElasticRetriever(getESClient, defaultESSettings.highlightFields, allSearchableIndices)

  // we must import the dsl

  import com.sksamuel.elastic4s.ElasticDsl._

  def getRelatedDiseases(id: String,
                         pagination: Option[Pagination]): Future[Option[DDRelations]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val kv = Map("A.keyword" -> id)

    val indexName = defaultESSettings.entities
      .find(_.name == "disease_relation")
      .map(_.index)
      .getOrElse("disease_relation")

    val aggs = Seq(
      valueCountAgg("relationCount", "B.keyword"),
      maxAgg("maxCountAOrB", "countAOrB")
    )

    val excludedFields = List("relatedInfo*")
    esRetriever
      .getByIndexedQuery(indexName,
                         kv,
                         pag,
                         fromJsValue[DDRelation],
                         aggs,
                         ElasticRetriever.sortByDesc("score"),
                         excludedFields)
      .map {
        case (Seq(), _) => None
        case (seq, agg) =>
          logger.debug(Json.prettyPrint(agg))
          val counts = (agg \ "relationCount" \ "value").as[Long]
          val maxCountAOrB = (agg \ "maxCountAOrB" \ "value").as[Long]
          Some(DDRelations(maxCountAOrB, counts, seq))
      }
  }

  def getRelatedTargets(id: String, pagination: Option[Pagination]): Future[Option[DDRelations]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val indexName = defaultESSettings.entities
      .find(_.name == "target_relation")
      .map(_.index)
      .getOrElse("target_relation")

    val kv = Map("A.keyword" -> id)

    val aggs = Seq(
      valueCountAgg("relationCount", "B.keyword"),
      maxAgg("maxCountAOrB", "countAOrB")
    )

    val excludedFields = List("relatedInfo*")
    esRetriever
      .getByIndexedQuery(indexName,
                         kv,
                         pag,
                         fromJsValue[DDRelation],
                         aggs,
                         ElasticRetriever.sortByDesc("score"),
                         excludedFields)
      .map {
        case (Seq(), _) => None
        case (seq, agg) =>
          logger.debug(Json.prettyPrint(agg))
          val counts = (agg \ "relationCount" \ "value").as[Long]
          val maxCountAOrB = (agg \ "maxCountAOrB" \ "value").as[Long]
          Some(DDRelations(maxCountAOrB, counts, seq))
      }
  }

  def getAdverseEvents(id: String,
                       pagination: Option[Pagination]): Future[Option[AdverseEvents]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val indexName = defaultESSettings.entities
      .find(_.name == "faers")
      .map(_.index)
      .getOrElse("faers")

    val kv = Map("chembl_id.keyword" -> id)

    val aggs = Seq(
      valueCountAgg("eventCount", "chembl_id.keyword")
    )

    esRetriever
      .getByIndexedQuery(indexName,
                         kv,
                         pag,
                         fromJsValue[AdverseEvent],
                         aggs,
                         ElasticRetriever.sortByDesc("llr"))
      .map {
        case (Seq(), _) => {
          logger.debug(s"No adverse event found for ${kv.toString}")
          None
        }
        case (seq, agg) =>
          logger.debug(Json.prettyPrint(agg))
          val counts = (agg \ "eventCount" \ "value").as[Long]
          Some(AdverseEvents(counts, seq.head.criticalValue, seq))
      }
  }

  def getCancerBiomarkers(id: String,
                          pagination: Option[Pagination]): Future[Option[CancerBiomarkers]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val cbIndex = defaultESSettings.entities
      .find(_.name == "cancerBiomarker")
      .map(_.index)
      .getOrElse("cancerbiomarkers")

    val kv = Map("target.keyword" -> id)

    val aggs = Seq(
      cardinalityAgg("uniqueDrugs", "drugName.keyword"),
      cardinalityAgg("uniqueDiseases", "disease.keyword"),
      cardinalityAgg("uniqueBiomarkers", "id.keyword"),
      valueCountAgg("rowsCount", "id.keyword")
    )

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

  def getKnownDrugs(queryString: String,
                    kv: Map[String, String],
                    sizeLimit: Option[Int],
                    cursor: Seq[String]): Future[Option[KnownDrugs]] = {

    val pag = Pagination(0, sizeLimit.getOrElse(Pagination.sizeDefault))
    val sortByField = sort.FieldSort(field = "phase").desc()
    val cbIndex = defaultESSettings.entities
      .find(_.name == "known_drugs")
      .map(_.index)
      .getOrElse("known_drugs")

    val aggs = Seq(
      cardinalityAgg("uniqueTargets", "targetId.raw"),
      cardinalityAgg("uniqueDiseases", "diseaseId.raw"),
      cardinalityAgg("uniqueDrugs", "drugId.raw"),
      valueCountAgg("rowsCount", "drugId.raw")
    )

    esRetriever
      .getByFreeQuery(cbIndex,
                      queryString,
                      kv,
                      pag,
                      fromJsValue[KnownDrug],
                      aggs,
                      Some(sortByField),
                      Seq("ancestors", "descendants"),
                      cursor)
      .map {
        case (Seq(), _, _) => None
        case (seq, agg, nextCursor) =>
          logger.debug(Json.prettyPrint(agg))
          val drugs = (agg \ "uniqueDrugs" \ "value").as[Long]
          val diseases = (agg \ "uniqueDiseases" \ "value").as[Long]
          val targets = (agg \ "uniqueTargets" \ "value").as[Long]
          val rowsCount = (agg \ "rowsCount" \ "value").as[Long]
          Some(KnownDrugs(drugs, diseases, targets, rowsCount, nextCursor, seq))
      }
  }

  // TODO CHECK RESULTS ARE SIZE 0 OR OPTIMISE FIELDS TO BRING BACK
  /** get evidences by multiple parameters */
  def getEvidences(datasourceIds: Option[Seq[String]],
                   targetIds: Seq[String],
                   diseaseIds: Seq[String],
                   orderBy: Option[(String, String)],
                   sizeLimit: Option[Int],
                   cursor: Option[Seq[String]]): Future[Evidences] = {

    val pag = sizeLimit.getOrElse(Pagination.sizeDefault)
    val sortByField = orderBy.flatMap { p =>
      ElasticRetriever.sortBy(p._1, if (p._2 == "desc") SortOrder.Desc else SortOrder.Asc)
    }

    val cbIndexPrefix = defaultESSettings.entities
      .find(_.name == "evidences")
      .map(_.index)
      .getOrElse("evidence_datasource_")

    val cbIndex = datasourceIds
      .map(_.map(cbIndexPrefix.concat).mkString(","))
      .getOrElse(cbIndexPrefix.concat("*"))

    val kv = Map(
      "targetId.keyword" -> targetIds,
      "diseaseId.keyword" -> diseaseIds
    )

    esRetriever
      .getByMustWithSearch(cbIndex,
                           None,
                           kv,
                           pag,
                           fromJsValue[JsValue],
                           Seq.empty,
                           sortByField,
                           Seq.empty,
                           cursor)
      .map {
        case (Seq(), n, _) => Evidences.empty(withTotal = n)
        case (seq, n, nextCursor) =>
          Evidences(n, nextCursor, seq)
      }
  }

  def getECOs(ids: Seq[String]): Future[IndexedSeq[ECO]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "eco")
      .map(_.index)
      .getOrElse("ecos")

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[ECO])
  }

  def getMousePhenotypes(ids: Seq[String]): Future[IndexedSeq[MousePhenotypes]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "mp")
      .map(_.index)
      .getOrElse("mp")

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[MousePhenotypes])
  }

  def getOtarProjects(ids: Seq[String]): Future[IndexedSeq[OtarProjects]] = {
    val otarsIndexName = defaultESSettings.entities
      .find(_.name == "otar_projects")
      .map(_.index)
      .getOrElse("otar_projects")

    esRetriever.getByIds(otarsIndexName, ids, fromJsValue[OtarProjects])
  }

  def getExpressions(ids: Seq[String]): Future[IndexedSeq[Expressions]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "expression")
      .map(_.index)
      .getOrElse("expression")

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Expressions])
  }

  def getReactomeNodes(ids: Seq[String]): Future[IndexedSeq[Reactome]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "reactome")
      .map(_.index)
      .getOrElse("reactome")

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Reactome])
  }

  def getTargets(ids: Seq[String]): Future[IndexedSeq[Target]] = {
    val targetIndexName = defaultESSettings.entities
      .find(_.name == "target")
      .map(_.index)
      .getOrElse("targets")

    val excludedFields = List("mousePhenotypes*")
    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Target], excludedFields = excludedFields)
  }

  /**
    * The linkedTargets field are mechanisms of action associated with a molecule. These should have the same behaviour
    * on either a parent or child molecule; this method performs that consolidation.
    * @param drug to query
    * @return consolidated LinkedIds
    */
  def getLinkedTargets(drug: Drug): Future[Option[LinkedIds]] = {
    def combinedLinkedTargets(drugs: IndexedSeq[Drug]): Option[LinkedIds] = {
      val linkedIds = drugs.flatMap(_.linkedTargets).flatMap(_.rows).distinct
      Some(LinkedIds(linkedIds.length, linkedIds))
    }
    drug.parentId match {
      // drug is a child so need to collect siblings
      case Some(parentId) =>
        logger.debug(s"Drug linked targets: Getting parent $parentId for ${drug.id}")
        getDrugs(Seq(parentId)) flatMap { d =>
          d.headOption match {
            case Some(parent) =>
              val family = parent.childChemblIds.getOrElse(Seq.empty)
              getDrugs(family) map { f =>
                combinedLinkedTargets(f ++ Seq(parent))
              }
            case None =>
              logger.warn(
                s"Drug linked targets: Parent $parentId for drug ${drug.id} was not resolved.")
              Future { drug.linkedTargets }
          }
        }
      case None =>
        drug.childChemblIds match {
          // drug is a parent, get children
          case Some(childrenIds) =>
            logger.trace(s"Drug linked targets: Getting children $childrenIds for ${drug.id}")
            getDrugs(childrenIds) map { c =>
              combinedLinkedTargets(c ++ Seq(drug))
            }
          // drug is an orphan
          case None => Future { drug.linkedTargets }
        }
    }
  }

  def getDrugs(ids: Seq[String]): Future[IndexedSeq[Drug]] = {
    val drugIndexName = defaultESSettings.entities
      .find(_.name == "drug")
      .map(_.index)
      .getOrElse("drugs")
    esRetriever.getByIds(drugIndexName, ids, fromJsValue[Drug])
  }

  def getMechanismsOfAction(ids: Seq[String]): Future[IndexedSeq[MechanismsOfAction]] = {
    val moaIndex = defaultESSettings.entities.find(_.name == "drugMoA").map(_.index)

    moaIndex match {
      case Some(idx) =>
        esRetriever.getByIds(idx, ids, fromJsValue[MechanismsOfAction])
      case None =>
        logger.error("Unable to resolve mechanism of action elasticsearch index!")
        Future { IndexedSeq.empty }
    }
  }

  def getIndications(ids: Seq[String]): Future[IndexedSeq[Indications]] = {
    logger.debug(s"querying ES: getting indications for $ids")
    val index = defaultESSettings.entities.find(_.name == "drugIndications").map(_.index)

    index match {
      case Some(idx) =>
        esRetriever.getByIds(idx, ids, fromJsValue[Indications])
      case None =>
        logger.error("Unable to resolve drug indications elasticsearch index!")
        Future { IndexedSeq.empty }
    }
  }

  def getDiseases(ids: Seq[String]): Future[IndexedSeq[Disease]] = {
    val diseaseIndexName = defaultESSettings.entities
      .find(_.name == "disease")
      .map(_.index)
      .getOrElse("diseases")

    esRetriever.getByIds(diseaseIndexName, ids, fromJsValue[Disease])
  }

  def search(qString: String,
             pagination: Option[Pagination],
             entityNames: Seq[String]): Future[SearchResults] = {
    val entities = for {
      e <- defaultESSettings.entities
      if (entityNames.contains(e.name) && e.searchIndex.isDefined)
    } yield e

    esRetriever.getSearchResultSet(entities, qString, pagination.getOrElse(Pagination.mkDefault))
  }

  def getAssociationDatasources: Future[Vector[EvidenceSource]] =
    dbRetriever.getUniqList[EvidenceSource](Seq("datasource_id", "datatype_id"),
                                            defaultOTSettings.clickhouse.disease.associations.name)

  def getAssociationsDiseaseFixed(disease: Disease,
                                  datasources: Option[Seq[DatasourceSettings]],
                                  indirect: Boolean,
                                  aggregationFilters: Seq[AggregationFilter],
                                  targetSet: Set[String],
                                  filter: Option[String],
                                  orderBy: Option[(String, String)],
                                  pagination: Option[Pagination]) = {
    val page = pagination.getOrElse(Pagination.mkDefault)
    val dss = datasources.getOrElse(defaultOTSettings.clickhouse.harmonic.datasources)

    val weights = dss.map(s => (s.id, s.weight))
    val dontPropagate = dss.withFilter(!_.propagate).map(_.id).toSet
    val aotfQ = QAOTF(defaultOTSettings.clickhouse.disease.associations.name,
                      disease.id,
                      _,
                      _,
                      filter,
                      orderBy,
                      weights,
                      dontPropagate,
                      page.offset,
                      page.size)

    logger.debug(s"get disease id ${disease.name}")
    val dIDs = disease.descendants.toSet + disease.id
    val indirectIDs = if (indirect) dIDs else Set.empty[String]
    val simpleQ = aotfQ(indirectIDs, targetSet).simpleQuery(0, 100000)

    val evidencesIndexName = defaultESSettings.entities
      .find(_.name == "evidences_aotf")
      .map(_.index)
      .getOrElse("evidences_aotf")

    val mappings = Map(
      "dataTypes" -> AggregationMapping("datatype_id",
                                        IndexedSeq("datatype_id", "datasource_id"),
                                        false),
      "tractabilitySmallmolecule" -> AggregationMapping("facet_tractability_smallmolecule",
                                                        IndexedSeq.empty,
                                                        false),
      "tractabilityAntibody" -> AggregationMapping("facet_tractability_antibody",
                                                   IndexedSeq.empty,
                                                   false),
      "pathwayTypes" -> AggregationMapping("facet_reactome", IndexedSeq("l1", "l2"), true),
      "targetClasses" -> AggregationMapping("facet_classes", IndexedSeq("l1", "l2"), true)
    )

    val queries = ElasticRetriever.aggregationFilterProducer(aggregationFilters, mappings)
    val filtersMap = queries._2

    val uniqueTargetsAgg =
      CardinalityAggregation("uniques", Some("target_id.keyword"), precisionThreshold = Some(40000))
    val reverseTargetsAgg = ReverseNestedAggregation("uniques", None, Seq(uniqueTargetsAgg))

    val queryAggs = Seq(
      FilterAggregation(
        "uniques",
        queries._1,
        subaggs =
          Seq(uniqueTargetsAgg,
              TermsAggregation("ids", field = Some("target_id.keyword"), size = Some(40000)))),
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
              TermsAggregation("aggs",
                               Some("datasource_id.keyword"),
                               size = Some(100),
                               subaggs = Seq(
                                 uniqueTargetsAgg
                               ))
            )
          )
        )
      ),
      FilterAggregation(
        "tractabilitySmallmolecule",
        filtersMap("tractabilitySmallmolecule"),
        subaggs = Seq(
          uniqueTargetsAgg,
          TermsAggregation("aggs",
                           Some("facet_tractability_smallmolecule.keyword"),
                           size = Some(100),
                           subaggs = Seq(
                             uniqueTargetsAgg
                           ))
        )
      ),
      FilterAggregation(
        "tractabilityAntibody",
        filtersMap("tractabilityAntibody"),
        subaggs = Seq(
          uniqueTargetsAgg,
          TermsAggregation("aggs",
                           Some("facet_tractability_antibody.keyword"),
                           size = Some(100),
                           subaggs = Seq(
                             uniqueTargetsAgg
                           ))
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
                  TermsAggregation("aggs",
                                   Some("facet_reactome.l2.keyword"),
                                   size = Some(100),
                                   subaggs = Seq(reverseTargetsAgg)),
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
                  TermsAggregation("aggs",
                                   Some("facet_classes.l2.keyword"),
                                   size = Some(100),
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

    val esQ = esRetriever.getAggregationsByQuery(
      evidencesIndexName,
      boolQuery()
        .withShould(
          boolQuery()
            .withMust(termsQuery("disease_id.keyword", indirectIDs))
            .withMust(not(termsQuery("datasource_id.keyword", dontPropagate))))
        .withShould(boolQuery()
          .withMust(termQuery("disease_id.keyword", disease.id))),
      queryAggs
    ) map {
      case obj: JsObject =>
        logger.debug(Json.prettyPrint(obj))

        val ids = (obj \ "uniques" \ "ids" \ "buckets" \\ "key").map(_.as[String]).toSet
        val uniques = (obj \ "uniques" \\ "value").head.as[Long]
        val restAggs = (obj - "uniques").fields map { pair =>
          NamedAggregation(pair._1,
                           (pair._2 \ "uniques" \\ "value").headOption.map(jv => jv.as[Long]),
                           (pair._2 \\ "buckets").head.as[Array[entities.Aggregation]])
        }

        Some((Aggregations(uniques, restAggs), ids))

      case _ => None
    }

    // TODO use option to enable or disable the computation of each of the sides
    (dbRetriever.executeQuery[String, Query](simpleQ) zip esQ) flatMap {
      case (tIDs, esR) =>
        val tids = esR.map(_._2 intersect tIDs.toSet).getOrElse(tIDs.toSet)
        val fullQ = aotfQ(indirectIDs, tids).query

        logger.debug(
          s"disease fixed get simpleQ n ${tIDs.size} " +
            s"agg n ${esR.map(_._2.size).getOrElse(-1)} " +
            s"inter n ${tids.size}")

        if (tids.nonEmpty) {
          dbRetriever.executeQuery[Association, Query](fullQ) map {
            case assocs => Associations(dss, esR.map(_._1), tids.size, assocs)
          }
        } else {
          Future.successful(Associations(dss, esR.map(_._1), tids.size, Vector.empty))
        }
    }
  }

  def getAssociationsTargetFixed(target: Target,
                                 datasources: Option[Seq[DatasourceSettings]],
                                 indirect: Boolean,
                                 aggregationFilters: Seq[AggregationFilter],
                                 diseaseSet: Set[String],
                                 filter: Option[String],
                                 orderBy: Option[(String, String)],
                                 pagination: Option[Pagination]): Future[Associations] = {
    val page = pagination.getOrElse(Pagination.mkDefault)
    val dss = datasources.getOrElse(defaultOTSettings.clickhouse.harmonic.datasources)

    val weights = dss.map(s => (s.id, s.weight))
    val dontPropagate = dss.withFilter(!_.propagate).map(_.id).toSet
    val aotfQ = QAOTF(defaultOTSettings.clickhouse.target.associations.name,
                      target.id,
                      _,
                      _,
                      filter,
                      orderBy,
                      weights,
                      dontPropagate,
                      page.offset,
                      page.size)

    logger.debug(s"get target id ${target.approvedSymbol} ACTUALLY DISABLED!")
    val indirectIDs = if (indirect) {
      val interactions = Interactions.find(target.id, None, pagination = Some(Pagination(0, 10000))) map {
        case Some(ints) =>
          ints.rows
            .withFilter(_("targetB").as[String].startsWith("ENSG"))
            .map(_("targetB").as[String])
            .toSet + target.id
        case None => Set.empty + target.id
      }

      interactions.await

    } else Set.empty[String]

    val simpleQ = aotfQ(indirectIDs, diseaseSet).simpleQuery(0, 100000)

    val evidencesIndexName = defaultESSettings.entities
      .find(_.name == "evidences_aotf")
      .map(_.index)
      .getOrElse("evidences_aotf")

    val mappings = Map(
      "dataTypes" -> AggregationMapping("datatype_id",
                                        IndexedSeq("datatype_id", "datasource_id"),
                                        false),
      "therapeuticAreas" -> AggregationMapping("facet_therapeuticAreas", IndexedSeq.empty, false)
    )

    val queries = ElasticRetriever.aggregationFilterProducer(aggregationFilters, mappings)
    val filtersMap = queries._2

    val uniqueDiseasesAgg = CardinalityAggregation("uniques",
                                                   Some("disease_id.keyword"),
                                                   precisionThreshold = Some(40000))

    val queryAggs = Seq(
      FilterAggregation(
        "uniques",
        queries._1,
        subaggs =
          Seq(uniqueDiseasesAgg,
              TermsAggregation("ids", field = Some("disease_id.keyword"), size = Some(40000)))),
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
              TermsAggregation("aggs",
                               Some("datasource_id.keyword"),
                               size = Some(100),
                               subaggs = Seq(
                                 uniqueDiseasesAgg
                               ))
            )
          )
        )
      ),
      FilterAggregation(
        "therapeuticAreas",
        filtersMap("therapeuticAreas"),
        subaggs = Seq(
          uniqueDiseasesAgg,
          TermsAggregation("aggs",
                           Some("facet_therapeuticAreas.keyword"),
                           size = Some(100),
                           subaggs = Seq(
                             uniqueDiseasesAgg
                           ))
        )
      )
    )

    val esQ = esRetriever.getAggregationsByQuery(
      evidencesIndexName,
      boolQuery()
        .withShould(
          boolQuery()
            .withMust(termsQuery("target_id.keyword", indirectIDs))
            .withMust(not(termsQuery("datasource_id.keyword", dontPropagate))))
        .withShould(boolQuery()
          .withMust(termQuery("target_id.keyword", target.id))),
      queryAggs
    ) map {
      case obj: JsObject =>
        logger.debug(Json.prettyPrint(obj))

        val ids = (obj \ "uniques" \ "ids" \ "buckets" \\ "key").map(_.as[String]).toSet
        val uniques = (obj \ "uniques" \\ "value").head.as[Long]
        val restAggs = (obj - "uniques").fields map { pair =>
          NamedAggregation(pair._1,
                           (pair._2 \ "uniques" \\ "value").headOption.map(jv => jv.as[Long]),
                           (pair._2 \\ "buckets").head.as[Array[entities.Aggregation]])
        }

        Some((Aggregations(uniques, restAggs), ids))

      case _ => None
    }

    (dbRetriever.executeQuery[String, Query](simpleQ) zip esQ) flatMap {
      case (dIDs, esR) =>
        val dids = esR.map(_._2 intersect dIDs.toSet).getOrElse(dIDs.toSet)
        val fullQ = aotfQ(indirectIDs, dids).query

        logger.debug(
          s"target fixed get simpleQ n ${dIDs.size} " +
            s"agg n ${esR.map(_._2.size).getOrElse(-1)} " +
            s"inter n ${dids.size}")

        if (dids.nonEmpty) {
          dbRetriever.executeQuery[Association, Query](fullQ) map {
            case assocs => Associations(dss, esR.map(_._1), dids.size, assocs)
          }
        } else {
          Future.successful(Associations(dss, esR.map(_._1), dids.size, Vector.empty))
        }
    }
  }
}
