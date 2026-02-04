package models

import clickhouse.ClickHouseProfile
import com.sksamuel.elastic4s.*
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.searches.*
import com.sksamuel.elastic4s.requests.searches.aggs.*
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import esecuele.*
import gql.validators.QueryTermsValidator.*

import javax.inject.Inject
import models.Helpers.*
import models.db.*
import models.entities.Publication.*
import models.entities.Associations.*
import models.entities.Biosample.*
import models.entities.CredibleSets.*
import models.entities.Colocalisations.*
import models.entities.Configuration.*
import models.entities.DiseaseHPOs.*
import models.entities.Drug.*
import models.entities.Interactions.*
import models.entities.Intervals.*
import models.entities.Loci.*
import models.entities.MousePhenotypes.*
import models.entities.Pharmacogenomics.*
import models.entities.ProteinCodingCoordinates.*
import models.entities.SearchFacetsResults.*
import models.entities.Studies.*
import models.entities.Evidence.*
import models.entities.SequenceOntologyTerm.*
import models.entities.*
import models.gql.{StudyTypeEnum, InteractionSourceEnum}
import org.apache.http.impl.nio.reactor.IOReactorConfig
import play.api.cache.AsyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.*
import play.api.{Configuration, Environment, Logging}
import play.db.NamedDatabase
import slick.basic.DatabaseConfig

import java.time.LocalDate
import scala.concurrent.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import models.entities.Violations.{DateFilterError, InputParameterCheckError}
import services.ApplicationStart
import utils.MetadataUtils.getIndexWithPrefixOrDefault
import models.gql.InteractionSourceEnum.InteractionSource
import com.sksamuel.elastic4s.requests.searches.suggestion.Fuzziness.One

class Backend @Inject() (implicit
    ec: ExecutionContext,
    @NamedDatabase("default") dbConfigProvider: DatabaseConfigProvider,
    appStart: ApplicationStart,
    config: Configuration,
    env: Environment,
    cache: AsyncCacheApi
) extends Logging {

  implicit val defaultOTSettings: OTSettings = loadConfigurationObject[OTSettings]("ot", config)
  implicit val defaultESSettings: ElasticsearchSettings = defaultOTSettings.elasticsearch
  implicit val dbConfig: DatabaseConfig[ClickHouseProfile] = dbConfigProvider.get[ClickHouseProfile]

  /** return meta information loaded from ot.meta settings */
  lazy val getMeta: Meta = defaultOTSettings.meta
  lazy val getESClient: ElasticClient = ElasticClient(
    JavaClient(
      ElasticProperties(s"http://${defaultESSettings.host}:${defaultESSettings.port}"),
      httpClientConfigCallback =
        _.setDefaultIOReactorConfig(IOReactorConfig.custom.setSoKeepAlive(true).build())
    )
  )
  val allSearchableIndices: Seq[String] = defaultESSettings.entities
    .withFilter(_.searchIndex.isDefined)
    .map(_.searchIndex.get)
    .map(getIndexWithPrefixOrDefault)

  val test = dbConfigProvider.get[ClickHouseProfile]

  implicit lazy val dbRetriever: ClickhouseRetriever =
    new ClickhouseRetriever(defaultOTSettings)

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
        case Results(Seq(), _, _, _) =>
          logger.debug(s"No adverse event found for ${kv.toString}")
          None
        case Results(seq, agg, _, _) =>
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
      case Results(Seq(), _, _, _) => Some(DiseaseHPOs(0, Seq()))
      case Results(seq, agg, _, _) =>
        logger.trace(Json.prettyPrint(agg))
        val rowsCount = (agg \ "rowsCount" \ "value").as[Long]
        Some(DiseaseHPOs(rowsCount, seq))
    }
  }

  def getDownloads: Future[Option[String]] = {
    val indexName = getIndexOrDefault("downloads")
    // We assume that the index has a single document, "croissant", with the downloads information
    esRetriever.getByIds(indexName, Seq("croissant"), fromJsValue[JsValue]).map {
      case IndexedSeq(downloads) =>
        Some(downloads.toString)
      case _ => None
    }
  }

  def getGoTerms(ids: Seq[String]): Future[IndexedSeq[GeneOntologyTerm]] = {
    val targetIndexName = getIndexOrDefault("go")

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[GeneOntologyTerm])
  }

  def getL2GPredictions(ids: Seq[String],
                        pagination: Option[Pagination]
  ): Future[IndexedSeq[L2GPredictions]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.l2gPredictions.name)
    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val l2gQuery = L2GQuery(
      ids,
      tableName,
      pag._1,
      pag._2
    )
    val results = dbRetriever
      .executeQuery[L2GPredictions, Query](l2gQuery.query)
    results
  }

  def getVariants(ids: Seq[String]): Future[IndexedSeq[VariantIndex]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.variant.name)
    val variantsQuery = IdsQuery(ids, "variantId", tableName, 0, Pagination.sizeMax)
    val results = dbRetriever
      .executeQuery[VariantIndex, Query](variantsQuery.query)
    results
  }

  def getBiosamples(ids: Seq[String]): Future[IndexedSeq[Biosample]] = {
    val indexName = getIndexOrDefault("biosample", Some("biosample"))
    esRetriever
      .getByIndexedTermsMust(
        indexName,
        Map("biosampleId.keyword" -> ids),
        Pagination.mkMax,
        fromJsValue[Biosample]
      )
      .map(_.mappedHits)
  }

  def getStudy(ids: Seq[String]): Future[IndexedSeq[Study]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.study.name)
    val studiesQuery = IdsQuery(ids, "studyId", tableName, 0, Pagination.sizeMax)
    val results = dbRetriever
      .executeQuery[Study, Query](studiesQuery.query)
    results
  }

  def getStudies(queryArgs: StudyQueryArgs, pagination: Option[Pagination]): Future[Studies] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.study.name)
    val diseaseTableName =
      getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.disease.name)
    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val studiesQuery = StudiesQuery(queryArgs, tableName, diseaseTableName, pag._1, pag._2)
    val results = dbRetriever
      .executeQuery[Study, Query](studiesQuery.query)
      .map { studies =>
        if (studies.isEmpty) {
          Studies.empty
        } else {
          Studies(studies.head.metaTotal, studies)
        }
      }
    results
  }

  def getColocalisations(studyLocusIds: Seq[String],
                         studyTypes: Seq[StudyTypeEnum.Value] = Seq(StudyTypeEnum.gwas),
                         pagination: Option[Pagination]
  ): Future[IndexedSeq[Colocalisations]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.colocalisation.name)
    val page = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val colocQuery = OneToManyQuery.colocQuery(
      studyLocusIds,
      studyTypes,
      tableName,
      page._1,
      page._2
    )
    val results =
      dbRetriever
        .executeQuery[Colocalisation, Query](colocQuery.query)
        .map { colocs =>
          studyLocusIds.map { studyLocusId =>
            val filteredColocs = colocs.filter(_.studyLocusId == studyLocusId)
            if (filteredColocs.nonEmpty) {
              Colocalisations(filteredColocs.head.metaTotal, filteredColocs, studyLocusId)
            } else {
              Colocalisations.empty
            }
          }.toIndexedSeq
        }
    results
  }

  def getLocus(studyLocusIds: Seq[String],
               variantIds: Option[Seq[String]],
               pagination: Option[Pagination]
  ): Future[IndexedSeq[Loci]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.credibleSet.locus.name)
    val page = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val locusQuery = LocusQuery(
      studyLocusIds,
      tableName,
      variantIds,
      page._1,
      page._2
    )
    val results = dbRetriever
      .executeQuery[Loci, Query](locusQuery.query)
    results
  }

  def getCredibleSet(ids: Seq[String]): Future[IndexedSeq[CredibleSet]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.credibleSet.name)
    val credsetQuery = IdsQuery(ids, "studyLocusId", tableName, 0, Pagination.sizeMax)
    val results = dbRetriever
      .executeQuery[CredibleSet, Query](credsetQuery.query)
    results
  }

  def getCredibleSets(
      queryArgs: CredibleSetQueryArgs,
      pagination: Option[Pagination]
  ): Future[CredibleSets] = {
    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.credibleSet.name)
    val studyTableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.study.name)
    val variantTableName = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.credibleSet.variant.name
    )
    val regionTableName = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.credibleSet.region.name
    )
    val credsetQuery = CredibleSetQuery(
      queryArgs,
      tableName,
      studyTableName,
      variantTableName,
      regionTableName,
      pag._1,
      pag._2
    )
    val results = dbRetriever
      .executeQuery[CredibleSet, Query](credsetQuery.query)
      .map { credsets =>
        if (credsets.isEmpty) {
          CredibleSets.empty
        } else {
          CredibleSets(credsets.head.metaTotal, credsets)
        }
      }
    results
  }

  def getCredibleSetsByStudy(studyIds: Seq[String],
                             pagination: Option[Pagination]
  ): Future[IndexedSeq[CredibleSets]] = {
    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.credibleSet.name)
    val studyTableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.study.name)
    val credsetQuery = CredibleSetByStudyQuery(
      studyIds,
      tableName,
      studyTableName,
      pag._1,
      pag._2
    )
    val results =
      dbRetriever
        .executeQuery[CredibleSet, Query](credsetQuery.query)
        .map { credsets =>
          studyIds.map { studyId =>
            val filteredCredsets = credsets.filter(_.studyId.contains(studyId))
            if (filteredCredsets.nonEmpty) {
              CredibleSets(filteredCredsets.head.metaTotal, filteredCredsets, studyId)
            } else {
              CredibleSets.empty
            }
          }.toIndexedSeq
        }
    results
  }

  def getCredibleSetsByVariant(variantIds: Seq[String],
                               studyTypes: Option[Seq[StudyTypeEnum.Value]],
                               pagination: Option[Pagination]
  ): Future[IndexedSeq[CredibleSets]] = {
    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.credibleSet.name)
    val variantTableName = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.credibleSet.variant.name
    )
    val credsetQuery = CredibleSetByVariantQuery(
      variantIds,
      studyTypes,
      tableName,
      variantTableName,
      pag._1,
      pag._2
    )
    val results =
      dbRetriever
        .executeQuery[CredibleSet, Query](credsetQuery.query)
        .map { credsets =>
          variantIds.map { variantId =>
            val filteredCredsets = credsets.filter(_.metaGroupId.contains(variantId))
            if (filteredCredsets.nonEmpty) {
              CredibleSets(filteredCredsets.head.metaTotal, filteredCredsets, variantId)
            } else {
              CredibleSets.empty
            }
          }.toIndexedSeq
        }
    results
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

      // transform the object in a key value pair array
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

  def getEvidencesByVariantId(
      datasourceIds: Option[Seq[String]],
      variantId: String,
      orderBy: Option[(String, String)],
      sizeLimit: Option[Int],
      cursor: Option[String]
  ): Future[Evidences] = {
    val evidenceTable = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.evidence.name)
    val variantJoinTable = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.evidence.variant.name
    )
    val pag = Helpers.Cursor
      .to(cursor)
      .flatMap(_.asOpt[Pagination])
      .getOrElse(Pagination(0, sizeLimit.getOrElse(Pagination.sizeDefault)))
    val evidenceQuery = EvidenceQuery.byVariant(
      variantId,
      datasourceIds,
      evidenceTable,
      variantJoinTable,
      pag.offset,
      pag.size
    )
    dbRetriever
      .executeQuery[Evidence, Query](evidenceQuery.query)
      .map { evidences =>
        if (evidences.isEmpty) {
          Evidences.empty(0)
        } else {
          val nCursor = if (evidences.size < pag.size) {
            None
          } else {
            val npag = pag.next
            Helpers.Cursor.from(Some(Json.toJson(npag)))
          }
          Evidences(evidences.head.metaTotal, nCursor, evidences)
        }
      }
  }

  def getEvidencesByEfoId(
      datasourceIds: Option[Seq[String]],
      targetIds: Seq[String],
      diseaseIds: Seq[String],
      orderBy: Option[(String, String)],
      sizeLimit: Option[Int],
      cursor: Option[String]
  ): Future[Evidences] = {
    val evidenceTable = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.evidence.name)
    val diseaseTargetJoinTable = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.evidence.diseaseAndTarget.name
    )
    val pag = Helpers.Cursor
      .to(cursor)
      .flatMap(_.asOpt[Pagination])
      .getOrElse(Pagination(0, sizeLimit.getOrElse(Pagination.sizeDefault)))
    val evidenceQuery = EvidenceQuery.byDiseaseTarget(
      targetIds,
      diseaseIds,
      datasourceIds,
      evidenceTable,
      diseaseTargetJoinTable,
      pag.offset,
      pag.size
    )
    dbRetriever
      .executeQuery[Evidence, Query](evidenceQuery.query)
      .map { evidences =>
        if (evidences.isEmpty) {
          Evidences.empty(0)
        } else {
          val nCursor = if (evidences.size < pag.size) {
            None
          } else {
            val npag = pag.next
            Helpers.Cursor.from(Some(Json.toJson(npag)))
          }
          Evidences(evidences.head.metaTotal, nCursor, evidences)
        }
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
      .map(_.mappedHits)
  }

  def getPharmacogenomicsByDrug(id: String): Future[IndexedSeq[Pharmacogenomics]] = {
    val queryTerm: Map[String, String] = Map("drugs.drugId.keyword" -> id)
    getPharmacogenomics(id, queryTerm)
  }

  def getPharmacogenomicsByTarget(id: String): Future[IndexedSeq[Pharmacogenomics]] = {
    val queryTerm: Map[String, String] = Map("targetFromSourceId.keyword" -> id)
    getPharmacogenomics(id, queryTerm)
  }

  def getPharmacogenomicsByVariant(id: String): Future[IndexedSeq[Pharmacogenomics]] = {
    val queryTerm: Map[String, String] = Map("variantId.keyword" -> id)
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
      .map(_.mappedHits)
  }

  def getProteinCodingCoordinatesByTarget(id: String,
                                          pagination: Option[Pagination]
  ): Future[ProteinCodingCoordinates] = {
    val queryTerm: Map[String, String] = Map("targetId.keyword" -> id)
    getProteinCodingCoordinates(id, queryTerm, pagination)
  }
  def getProteinCodingCoordinatesByVariantId(id: String,
                                             pagination: Option[Pagination]
  ): Future[ProteinCodingCoordinates] = {
    val queryTerm: Map[String, String] = Map("variantId.keyword" -> id)
    getProteinCodingCoordinates(id, queryTerm, pagination)
  }
  def getProteinCodingCoordinates(id: String,
                                  queryTerm: Map[String, String],
                                  pagination: Option[Pagination]
  ): Future[ProteinCodingCoordinates] = {
    val indexName = getIndexOrDefault("proteinCodingCoordinates")
    val pag = pagination.getOrElse(Pagination(0, 2))
    logger.debug(s"Querying protein coding coordinates for: $id")
    val retriever = esRetriever
      .getByIndexedQueryMust(
        indexName,
        queryTerm,
        pag,
        fromJsValue[ProteinCodingCoordinate]
      )
    retriever.map {
      case Results(Seq(), _, _, _) => ProteinCodingCoordinates.empty()
      case Results(coords, _, counts, _) =>
        ProteinCodingCoordinates(counts, coords)
    }
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
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.target.name)
    val targetsQuery = IdsQuery(ids, "id", tableName, 0, Pagination.sizeMax)
    val results = dbRetriever
      .executeQuery[Target, Query](targetsQuery.query)
    results
  }

  def getSoTerms(ids: Seq[String]): Future[IndexedSeq[SequenceOntologyTerm]] = {
    val targetIndexName = getIndexOrDefault("so", Some("so"))

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[SequenceOntologyTerm])
  }

  def getDrugs(ids: Seq[String]): Future[IndexedSeq[Drug]] = {
    logger.debug(s"Querying drugs: $ids")
    val drugIndexName = getIndexOrDefault("drug")
    val queryTerm = Map("id.keyword" -> ids)
    esRetriever
      .getByIndexedQueryShould(drugIndexName, queryTerm, Pagination(0, ids.size), fromJsValue[Drug])
      .map(_.mappedHits)
  }

  def getMechanismsOfAction(id: String): Future[MechanismsOfAction] = {

    logger.debug(s"querying ES: getting mechanisms of action for $id")
    val index = getIndexOrDefault("drugMoA")
    val queryTerms = Map("chemblIds.keyword" -> id)
    val mechanismsOfActionRaw: Future[Results[MechanismOfActionRaw]] =
      esRetriever.getByIndexedQueryShould(
        index,
        queryTerms,
        Pagination.mkDefault,
        fromJsValue[MechanismOfActionRaw]
      )
    mechanismsOfActionRaw.map(i => Drug.mechanismOfActionRaw2MechanismOfAction(i.mappedHits))
  }

  def getIndications(ids: Seq[String]): Future[IndexedSeq[Indications]] = {
    logger.debug(s"querying ES: getting indications for $ids")
    val index = getIndexOrDefault("drugIndications")
    val queryTerm = Map("id.keyword" -> ids)

    esRetriever
      .getByIndexedQueryShould(index, queryTerm, Pagination.mkDefault, fromJsValue[Indications])
      .map(_.mappedHits)
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
          results.mappedHits.foldLeft(Map.empty[(Option[Long]), DrugWarning]) { (dwMap, dw) =>
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
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.disease.name)
    val diseaseQuery = IdsQuery(ids, "id", tableName, 0, Pagination.sizeMax)
    val results = dbRetriever
      .executeQuery[Disease, Query](diseaseQuery.query)
    results
  }

  def getInteractions(ids: Seq[String],
                      scoreThreshold: Option[Double],
                      databaseName: Option[InteractionSourceEnum.Value],
                      pagination: Option[Pagination]
  ): Future[IndexedSeq[Interactions]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.interaction.name)
    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val interactionsQuery = OneToManyQuery.interactionQuery(
      ids,
      tableName,
      scoreThreshold,
      databaseName,
      pag._1,
      pag._2
    )
    val results = dbRetriever
      .executeQuery[Interaction, Query](interactionsQuery.query)
      .map { interactions =>
        ids.map { targetId =>
          val filteredInteractions = interactions.filter(_.targetA == targetId)
          if (filteredInteractions.nonEmpty) {
            Interactions(filteredInteractions.head.metaTotal, filteredInteractions, targetId)
          } else {
            Interactions.empty
          }
        }.toIndexedSeq
      }
    results
  }

  def getInteractionSources: Future[Seq[InteractionResources]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.interaction.name)
    val interactionSourcesQuery = InteractionSourcesQuery(tableName)
    val results = dbRetriever
      .executeQuery[InteractionResources, Query](interactionSourcesQuery.query)
    results
  }

  def getIntervals(chromosome: String,
                   start: Int,
                   end: Int,
                   pagination: Option[Pagination]
  ): Future[Intervals] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.intervals.name)
    val page = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val intervalsQuery = IntervalsQuery(
      chromosome,
      start,
      end,
      tableName,
      page._1,
      page._2
    )
    val results =
      dbRetriever
        .executeQuery[Interval, Query](intervalsQuery.query)
        .map { intervals =>
          if (intervals.length) > 0 then {
            Intervals(intervals.head.meta_total, intervals)
          } else {
            Intervals.empty
          }
        }
    results
  }

  def mapIds(
      queryTerms: Seq[String],
      entityNames: Seq[String]
  ): Future[MappingResults] = {

    val entities = for {
      e <- defaultESSettings.entities
      if (entityNames.contains(e.name) && e.searchIndex.isDefined)
    } yield e.copy(searchIndex = Some(getIndexWithPrefixOrDefault(e.searchIndex.getOrElse(""))))
    esRetriever.getTermsResultsMapping(entities, queryTerms)
  }

  def search(
      qString: String,
      pagination: Option[Pagination],
      entityNames: Seq[String]
  ): Future[SearchResults] = {
    val entities = entityNames match {
      case Nil => defaultESSettings.entities.filter(_.searchIndex.isDefined)
      case _ =>
        defaultESSettings.entities
          .filter(e => entityNames.contains(e.name) && e.searchIndex.isDefined)
    } map (e =>
      e.copy(searchIndex = Some(getIndexWithPrefixOrDefault(e.searchIndex.getOrElse(""))))
    )
    esRetriever.getSearchResultSet(entities, qString, pagination.getOrElse(Pagination.mkDefault))
  }

  def searchFacets(
      qString: String,
      pagination: Option[Pagination],
      entityNames: Seq[String],
      category: Option[String]
  ): Future[SearchFacetsResults] = {
    val entities = entityNames match {
      case Nil => defaultESSettings.entities.filter(_.facetSearchIndex.isDefined)
      case _ =>
        defaultESSettings.entities
          .filter(e => entityNames.contains(e.name) && e.facetSearchIndex.isDefined)
    } map (e =>
      e.copy(searchIndex = Some(getIndexWithPrefixOrDefault(e.searchIndex.getOrElse(""))))
    )
    esRetriever.getSearchFacetsResultSet(entities,
                                         qString,
                                         pagination.getOrElse(Pagination.mkDefault),
                                         category
    )
  }

  def getAssociationDatasources: Future[Vector[EvidenceSource]] =
    dbRetriever.getUniqList[EvidenceSource](
      Seq("datasource_id", "datatype_id"),
      getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.disease.associations.name)
    )

  def getAssociationsEntityFixed(
      tableName: String,
      datasources: Option[Seq[DatasourceSettings]],
      fixedEntityId: String,
      indirectIds: Set[String],
      bIds: Set[String],
      columnFilters: Seq[(String, Any)],
      filter: Option[String],
      orderBy: Option[(String, String)],
      pagination: Option[Pagination]
  ): Future[Associations] = {
    val page = pagination.getOrElse(Pagination.mkDefault)
    val dss = datasources.getOrElse(defaultOTSettings.clickhouse.harmonic.datasources)
    val weights = dss.map(s => (s.id, s.weight))
    val mustIncludeDatasources = dss.withFilter(_.required).map(_.id).toSet
    val dontPropagate = dss.withFilter(!_.propagate).map(_.id).toSet
    val aotfQ = QAOTF(
      tableName,
      fixedEntityId,
      _,
      _,
      filter,
      columnFilters,
      orderBy,
      weights,
      _,
      dontPropagate,
      page.offset,
      page.size
    )
    val simpleQ = aotfQ(indirectIds, bIds, mustIncludeDatasources).simpleQuery(0, 100000)

    (dbRetriever.executeQuery[String, Query](simpleQ)) flatMap { case assocIds =>
      val assocIdSet = assocIds.toSet
      val fullQ = aotfQ(indirectIds, assocIdSet, Set.empty).query

      if (assocIdSet.nonEmpty) {
        dbRetriever.executeQuery[Association, Query](fullQ) map { case assocs =>
          val filteredAssocs =
            if (mustIncludeDatasources.isEmpty) {
              assocs
            } else {
              assocs.flatMap { assoc =>
                val filteredDS =
                  assoc.datasourceScores.filter(ds => mustIncludeDatasources.contains(ds.id))
                if (filteredDS.isEmpty) None
                else Some(assoc)
              }
            }
          Associations(dss, assocIdSet.size, filteredAssocs)
        }
      } else {
        Future.successful(Associations(dss, assocIdSet.size, Vector.empty))
      }
    }
  }

  def getAssociationsDiseaseFixed(
      disease: Disease,
      datasources: Option[Seq[DatasourceSettings]],
      indirect: Boolean,
      facetFilters: Seq[String],
      targetSet: Set[String],
      filter: Option[String],
      orderBy: Option[(String, String)],
      pagination: Option[Pagination]
  ): Future[Associations] = {
    logger.debug(s"get disease id ${disease.name}")
    val indirectIDs = if (indirect) disease.descendants.toSet + disease.id else Set.empty[String]
    val targetIds = applyFacetFiltersToBIDs("facet_search_target", targetSet, facetFilters)
    getAssociationsEntityFixed(
      getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.disease.associations.name),
      datasources,
      disease.id,
      indirectIDs,
      targetIds,
      Seq.empty,
      filter,
      orderBy,
      pagination
    )
  }

  def getAssociationsTargetFixed(
      target: Target,
      datasources: Option[Seq[DatasourceSettings]],
      indirect: Boolean,
      includeMeasurements: Boolean,
      facetFilters: Seq[String],
      diseaseSet: Set[String],
      filter: Option[String],
      orderBy: Option[(String, String)],
      pagination: Option[Pagination]
  ): Future[Associations] = {
    logger.debug(s"get target id ${target.approvedSymbol} ACTUALLY DISABLED!")
    val indirectIDs = if (indirect) {
      val interactions =
        getInteractions(Seq(target.id), None, None, Some(Pagination(0, 10000))).map {
          case Seq() => Set.empty + target.id
          case Seq(intr) =>
            intr.rows
              .flatMap(int => int.targetB.filter(_.startsWith("ENSG")))
              .toSet + target.id
        }
      interactions.await
    } else Set.empty[String]

    val columnFilters = if (includeMeasurements == false) {
      Seq(("is_measurement", false))
    } else {
      Seq.empty
    }
    val diseaseIds =
      applyFacetFiltersToBIDs("facet_search_disease", diseaseSet, facetFilters)
    getAssociationsEntityFixed(
      getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.target.associations.name),
      datasources,
      target.id,
      indirectIDs,
      diseaseIds,
      columnFilters,
      filter,
      orderBy,
      pagination
    )
  }

  def getSimilarW2VEntities(
      label: String,
      labels: Set[String],
      categories: List[String],
      threshold: Double,
      size: Int
  ): Future[Vector[Similarity]] = {
    val table = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.similarities.name)
    logger.debug(s"query similarities in table ${table}")

    val jointLabels = labels + label
    val simQ = QW2V(table, categories, jointLabels, threshold, size)
    dbRetriever.executeQuery[Long, Query](simQ.existsLabel(label)).flatMap {
      case Vector(1) => dbRetriever.executeQuery[Similarity, Query](simQ.query)
      case _ =>
        logger.debug(
          s"This case where the label asked ${label} to the model Word2Vec does not exist" +
            s" is ok but nice to capture though"
        )
        Future.successful(Vector.empty)
    }
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
    val table = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.literature.name)
    val indexTable = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.literatureIndex.name)
    logger.debug(s"query literature ocurrences in table ${table} and index ${indexTable}")

    val pag = Helpers.Cursor.to(cursor).flatMap(_.asOpt[Pagination]).getOrElse(Pagination.mkDefault)

    val filterStartDate = (startYear, startMonth) match {
      case (Some(strYear), Some(strMonth)) =>
        Some(strYear, strMonth)
      case (Some(strYear), None) => Some(strYear, 1)
      case (None, Some(strMonth)) =>
        throw InputParameterCheckError(Vector(DateFilterError("startYear", "startMonth")))
      case _ => Option.empty
    }

    val filterEndDate = (endYear, endMonth) match {
      case (Some(ndYear), Some(ndMonth)) =>
        Some(ndYear, ndMonth)
      case (Some(ndYear), None) => Some(ndYear, 12)
      case (None, Some(ndMonth)) =>
        throw InputParameterCheckError(Vector(DateFilterError("startYear", "startMonth")))
      case _ => Option.empty
    }

    val simQ = QLITAGG(table, indexTable, ids, pag.size, pag.offset, filterStartDate, filterEndDate)

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
            logger.debug(s"Cannot find the earliest year for the publications.")
            runQuery(1900, total)
        }

      case _ =>
        logger.debug(s"there is no publications with this set of ids $ids")
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
    *   key of index (name field) in application.conf
    * @param default
    *   fallback index name
    * @return
    *   elasticsearch index name resolved from application.conf or default.
    */
  private def getIndexOrDefault(index: String, default: Option[String] = None): String =
    val indexName = defaultESSettings.entities
      .find(_.name == index)
      .map(_.index)
      .getOrElse(default.getOrElse(index))
    getIndexWithPrefixOrDefault(indexName)

  /** Get ClickHouse table name with the data prefix if enabled.
    * @param table
    *   table name
    * @return
    *   table name with data prefix if enabled, otherwise the table name as is.
    */
  private def getTableWithPrefixOrDefault(table: String): String =
    if (getMeta.enableDataReleasePrefix)
      getMeta.dataPrefix + "." + table
    else
      defaultOTSettings.clickhouse.defaultDatabaseName + "." + table

  /** Get the entity ids for a given set of facet filters.
    * @return
    *   A sequence of entity id sets.
    */
  private def resolveEntityIdsFromFacets(facetFilters: Seq[String],
                                         index: String
  ): Seq[Set[String]] = {
    val facets =
      esRetriever.getByIds(getIndexOrDefault(index), facetFilters, fromJsValue[Facet])
    val entityIdsGroupedByCategory =
      facets.await.groupMap(_.category)(_.entityIds.getOrElse(Set.empty))
    entityIdsGroupedByCategory.map(_._2.flatten.toSet).toSeq
  }

  /** Reduce a set of BIDs with the BIDs derived from the facets. If the set of BIDs is empty, the
    * BIDs are derived from the facets. If the set of facets is empty, the BIDs are returned as is.
    * If both the set of BIDs and the set of facets are not empty, the BIDs are intersected with the
    * BIDs derived from the facets. If the intersection is empty, a Set of "" is returned to ensure
    * that no ids are returned.
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
      val entityIdsFromFacets: Set[String] =
        resolveEntityIdsFromFacets(facetFilters, index).reduce(_ intersect _)
      val entityIdsFromFacetsIntersect: Set[String] =
        if (entityIdsFromFacets.isEmpty) Set.empty
        else entityIdsFromFacets
      if (bIDs.isEmpty) emptySetToSetOfEmptyString(entityIdsFromFacetsIntersect)
      else emptySetToSetOfEmptyString(bIDs.intersect(entityIdsFromFacetsIntersect))
    }

}
