package models

import clickhouse.ClickHouseProfile
import net.logstash.logback.argument.StructuredArguments.keyValue
import com.sksamuel.elastic4s.*
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.searches.*
import com.sksamuel.elastic4s.requests.searches.aggs.*
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import esecuele.*

import javax.inject.Inject
import models.Helpers.*
import models.db.*
import models.entities.Publication.*
import models.entities.AdverseEvent.*
import models.entities.Associations.*
import models.entities.Biosample.*
import models.entities.CredibleSets.*
import models.entities.Colocalisations.*
import models.entities.Configuration.*
import models.entities.DiseaseHPOs.*
import models.entities.Drug.*
import models.entities.DrugWarning.*
import models.entities.EnhancerToGenes.*
import models.entities.Interactions.*
import models.entities.Loci.*
import models.entities.MechanismsOfAction.*
import models.entities.MousePhenotypes.*
import models.entities.Pharmacogenomics.*
import models.entities.SearchFacetsResults.*
import models.entities.Studies.*
import models.entities.Evidences.*
import models.entities.SequenceOntologyTerm.*
import models.entities.*
import models.gql.{StudyTypeEnum, InteractionSourceEnum}
import models.entities.Violations.{DateFilterError, InputParameterCheckError}

import org.apache.http.impl.nio.reactor.IOReactorConfig
import play.api.cache.AsyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.*
import play.api.{Configuration, Environment}
import play.db.NamedDatabase
import slick.basic.DatabaseConfig
import java.time.LocalDate
import scala.concurrent.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import services.ApplicationStart

import utils.MetadataUtils.getIndexWithPrefixOrDefault
import utils.OTLogging

class Backend @Inject() (implicit
    ec: ExecutionContext,
    @NamedDatabase("default") dbConfigProvider: DatabaseConfigProvider,
    appStart: ApplicationStart,
    config: Configuration,
    env: Environment,
    cache: AsyncCacheApi
) extends OTLogging {

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
      ids: Seq[String],
      pagination: Option[Pagination]
  ): Future[IndexedSeq[AdverseEvents]] = {
    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.faers.name)
    logger.debug(s"querying adverse events", keyValue("ids", ids), keyValue("table", tableName))
    val query = OneToMany(
      ids,
      "chembl_id",
      "adverse_events",
      tableName,
      pag._1,
      pag._2,
      selectAlso = Seq(Column("criticalValue"))
    )
    dbRetriever
      .executeQuery[AdverseEvents, Query](query.query)
  }

  def getDiseaseHPOs(ids: Seq[String],
                     pagination: Option[Pagination]
  ): Future[IndexedSeq[DiseaseHPOs]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.diseaseHPO.name)
    logger.debug(s"querying disease hpos", keyValue("id", ids), keyValue("table", tableName))
    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val diseaseHPOQuery = OneToMany(
      ids = ids,
      idField = "disease",
      arrayField = "phenotypes",
      tableName = tableName,
      offset = pag._1,
      size = pag._2
    )
    dbRetriever.executeQuery[DiseaseHPOs, Query](diseaseHPOQuery.query)
  }

  def getDownloads: Future[Option[String]] = {
    val indexName = getIndexOrDefault("downloads")
    logger.debug(s"querying downloads", keyValue("index", indexName))
    // We assume that the index has a single document, "croissant", with the downloads information
    esRetriever.getByIds(indexName, Seq("croissant"), fromJsValue[JsValue]).map {
      case IndexedSeq(downloads) =>
        Some(downloads.toString)
      case _ => None
    }
  }

  def getGoTerms(ids: Seq[String]): Future[IndexedSeq[GeneOntologyTerm]] = {
    val targetIndexName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.go.name)
    logger.debug(s"querying go terms", keyValue("ids", ids), keyValue("table", targetIndexName))
    val query = IdsQuery(ids, "id", targetIndexName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[GeneOntologyTerm, Query](query.query)
  }

  def getL2GPredictions(ids: Seq[String],
                        pagination: Option[Pagination]
  ): Future[IndexedSeq[L2GPredictions]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.l2gPredictions.name)

    logger.debug(s"querying l2g predictions", keyValue("ids", ids), keyValue("table", tableName))

    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val l2gQuery = OneToMany.l2gQuery(
      ids,
      tableName,
      pag._1,
      pag._2
    )
    dbRetriever.executeQuery[L2GPredictions, Query](l2gQuery.query)
  }

  def getVariants(ids: Seq[String]): Future[IndexedSeq[VariantIndex]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.variant.name)

    logger.debug(s"querying variants", keyValue("ids", ids), keyValue("table", tableName))

    val variantsQuery = IdsQuery(ids, "variantId", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[VariantIndex, Query](variantsQuery.query)
  }

  def getBiosamples(ids: Seq[String]): Future[IndexedSeq[Biosample]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.biosample.name)
    logger.debug(s"querying biosamples", keyValue("ids", ids), keyValue("table", tableName))
    val query = IdsQuery(ids, "biosampleId", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[Biosample, Query](query.query)
  }

  def getStudy(ids: Seq[String]): Future[IndexedSeq[Study]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.study.name)

    logger.debug(s"querying studies by id", keyValue("ids", ids), keyValue("table", tableName))

    val studiesQuery = IdsQuery(ids, "studyId", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[Study, Query](studiesQuery.query)
  }

  def getStudies(queryArgs: StudyQueryArgs, pagination: Option[Pagination]): Future[Studies] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.study.name)
    logger.debug(s"querying studies by disease",
                 keyValue("id", queryArgs.id),
                 keyValue("diseases", queryArgs.diseaseIds),
                 keyValue("table", tableName)
    )
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

    logger.debug(s"querying colocalisations",
                 keyValue("study_locus_ids", studyLocusIds),
                 keyValue("studyType", studyTypes),
                 keyValue("table", tableName)
    )

    val page = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val colocQuery = OneToMany.colocQuery(
      studyLocusIds,
      studyTypes,
      tableName,
      page._1,
      page._2
    )
    dbRetriever.executeQuery[Colocalisations, Query](colocQuery.query)
  }

  def getLocus(studyLocusIds: Seq[String],
               variantIds: Option[Seq[String]],
               pagination: Option[Pagination]
  ): Future[IndexedSeq[Loci]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.credibleSet.locus.name)

    logger.debug(s"querying locus",
                 keyValue("ids", studyLocusIds),
                 keyValue("variant_ids", variantIds),
                 keyValue("table", tableName)
    )

    val page = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val locusQuery = OneToMany.locusQuery(
      studyLocusIds,
      tableName,
      variantIds,
      page._1,
      page._2
    )
    dbRetriever.executeQuery[Loci, Query](locusQuery.query)
  }

  def getCredibleSet(ids: Seq[String]): Future[IndexedSeq[CredibleSet]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.credibleSet.name)

    logger.debug(s"querying credible sets", keyValue("ids", ids), keyValue("table", tableName))

    val credsetQuery = IdsQuery(ids, "studyLocusId", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[CredibleSet, Query](credsetQuery.query)
  }

  def getCredibleSets(
      queryArgs: CredibleSetQueryArgs,
      pagination: Option[Pagination]
  ): Future[CredibleSets] = {
    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.credibleSet.name)
    logger.debug(
      s"querying credible sets",
      keyValue("studyLocusId", queryArgs.ids),
      keyValue("studyId", queryArgs.studyIds),
      keyValue("studyType", queryArgs.studyTypes),
      keyValue("region", queryArgs.regions),
      keyValue("table", tableName)
    )
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
    logger.debug(s"querying credible sets by study ids",
                 keyValue("ids", studyIds),
                 keyValue("table", tableName)
    )
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
    logger.debug(s"querying credible sets by variant ids",
                 keyValue("ids", variantIds),
                 keyValue("study_types", studyTypes),
                 keyValue("table", tableName)
    )
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
    val targetIndexName = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.target.essentiality.name
    )
    logger.debug(s"querying target essentiality",
                 keyValue("ids", ids),
                 keyValue("table", targetIndexName)
    )
    val query = IdsQuery(ids, "id", targetIndexName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[TargetEssentiality, Query](query.query)
  }

  def getTargetPrioritisation(ids: Seq[String]): Future[IndexedSeq[TargetPrioritisation]] = {
    val tableName = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.target.prioritisation.name
    )
    logger.debug(s"querying targets prioritisation",
                 keyValue("ids", ids),
                 keyValue("table", tableName)
    )
    val query = IdsQuery(ids, "targetId", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[TargetPrioritisation, Query](query.query)
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

    val mappedValues =
      Seq(keyValue("index", cbIndex)) ++ kv.map(pair => keyValue(pair._1, pair._2)).toSeq

    logger.debug(s"querying known drugs", mappedValues*)

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
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.hpo.name)
    logger.debug(s"querying hpos", keyValue("ids", ids), keyValue("table", tableName))
    val query = IdsQuery(ids, "id", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[HPO, Query](query.query)
  }

  def getMousePhenotypes(ids: Seq[String]): Future[IndexedSeq[MousePhenotypes]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.mousePhenotypes.name)
    logger.debug(s"querying mouse phenotypes", keyValue("ids", ids), keyValue("table", tableName))
    val query = OneToMany(
      ids,
      "targetFromSourceId",
      "mouse_phenotypes",
      tableName,
      0,
      Pagination.sizeMax
    )
    dbRetriever.executeQuery[MousePhenotypes, Query](query.query)
  }

  def getPharmacogenomicsByDrug(ids: Seq[String]): Future[IndexedSeq[PharmacogenomicsByDrug]] = {
    val tableName = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.pharmacogenomics.drug.name
    )
    logger.info(s"querying pharmacogenomics by drug",
                keyValue("ids", ids),
                keyValue("table", tableName)
    )
    val query = IdsQuery(ids, "drugId", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[PharmacogenomicsByDrug, Query](query.query)
  }

  def getPharmacogenomicsByTarget(
      ids: Seq[String]
  ): Future[IndexedSeq[PharmacogenomicsByTarget]] = {
    val tableName = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.pharmacogenomics.target.name
    )
    logger.debug(s"querying pharmacogenomics by target",
                 keyValue("ids", ids),
                 keyValue("table", tableName)
    )
    val query = IdsQuery(ids, "targetFromSourceId", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[PharmacogenomicsByTarget, Query](query.query)
  }

  def getPharmacogenomicsByVariant(
      ids: Seq[String]
  ): Future[IndexedSeq[PharmacogenomicsByVariant]] = {
    val tableName = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.pharmacogenomics.variant.name
    )
    logger.debug(s"querying pharmacogenomics by variant",
                 keyValue("ids", ids),
                 keyValue("table", tableName)
    )
    val query = IdsQuery(ids, "variantId", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[PharmacogenomicsByVariant, Query](query.query)
  }

  def getProteinCodingCoordinatesByTarget(ids: Seq[String],
                                          pagination: Option[Pagination]
  ): Future[IndexedSeq[ProteinCodingCoordinates]] = {
    val tableName = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.proteinCodingCoordinates.target.name
    )
    val pag = pagination.getOrElse(Pagination(0, 2)).offsetLimit
    val query = OneToMany(
      ids,
      "targetId",
      "proteinCodingCoords",
      tableName,
      pag._1,
      pag._2
    )
    dbRetriever.executeQuery[ProteinCodingCoordinates, Query](query.query)
  }
  def getProteinCodingCoordinatesByVariant(ids: Seq[String],
                                           pagination: Option[Pagination]
  ): Future[IndexedSeq[ProteinCodingCoordinates]] = {
    val tableName = getTableWithPrefixOrDefault(
      defaultOTSettings.clickhouse.proteinCodingCoordinates.variant.name
    )
    logger.debug(s"querying protein coding coordinates",
                 keyValue("ids", ids),
                 keyValue("table", tableName)
    )
    val pag = pagination.getOrElse(Pagination(0, 2)).offsetLimit
    val query = OneToMany(
      ids,
      "variantId",
      "proteinCodingCoords",
      tableName,
      pag._1,
      pag._2
    )
    dbRetriever.executeQuery[ProteinCodingCoordinates, Query](query.query)
  }

  def getOtarProjects(ids: Seq[String]): Future[IndexedSeq[OtarProjects]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.otarProjects.name)
    val query = IdsQuery(ids, "efo_id", tableName, 0, Pagination.sizeMax)
    logger.debug(s"querying otar projects", keyValue("ids", ids), keyValue("table", tableName))
    dbRetriever.executeQuery[OtarProjects, Query](query.query)
  }

  def getExpressions(ids: Seq[String]): Future[IndexedSeq[Expressions]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.expression.name)
    logger.debug(s"querying expressions", keyValue("ids", ids), keyValue("table", tableName))
    val expressionQuery = IdsQuery(ids, "id", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[Expressions, Query](expressionQuery.query)
  }

  def getTargets(ids: Seq[String]): Future[IndexedSeq[Target]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.target.name)
    val targetsQuery = IdsQuery(ids, "id", tableName, 0, Pagination.sizeMax)
    logger.debug(s"querying targets", keyValue("ids", ids), keyValue("table", tableName))
    dbRetriever.executeQuery[Target, Query](targetsQuery.query)
  }

  def getSoTerms(ids: Seq[String]): Future[IndexedSeq[SequenceOntologyTerm]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.so.name)
    logger.debug(s"querying so terms", keyValue("ids", ids), keyValue("table", tableName))
    val query = IdsQuery(ids, "id", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[SequenceOntologyTerm, Query](query.query)
  }

  def getDrugs(ids: Seq[String]): Future[IndexedSeq[Drug]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.drug.name)
    logger.debug(s"querying drugs", keyValue("ids", ids), keyValue("table", tableName))
    val query = IdsQuery(ids, "id", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[Drug, Query](query.query)
  }

  def getMechanismsOfAction(ids: Seq[String]): Future[IndexedSeq[MechanismsOfAction]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.mechanismOfAction.name)
    logger.debug(s"querying mechanisms of action",
                 keyValue("ids", ids),
                 keyValue("table", tableName)
    )
    val query = IdsQuery(ids, "chemblId", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[MechanismsOfAction, Query](query.query)
  }

  def getIndications(ids: Seq[String]): Future[IndexedSeq[Indications]] = {
    val index = getIndexOrDefault("drugIndications")
    logger.debug(s"querying indications", keyValue("ids", ids), keyValue("index", index))
    val queryTerm = Map("id.keyword" -> ids)

    esRetriever
      .getByIndexedQueryShould(index, queryTerm, Pagination.mkDefault, fromJsValue[Indications])
      .map(_.mappedHits)
  }

  def getDrugWarnings(ids: Seq[String]): Future[IndexedSeq[DrugWarnings]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.drugWarnings.name)
    logger.debug(s"querying drug warnings", keyValue("ids", ids), keyValue("table", tableName))
    val query = IdsQuery(ids, "chemblId", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[DrugWarnings, Query](query.query)
  }

  def getDiseases(ids: Seq[String]): Future[IndexedSeq[Disease]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.disease.name)
    logger.debug(s"querying diseases", keyValue("ids", ids), keyValue("table", tableName))
    val diseaseQuery = IdsQuery(ids, "id", tableName, 0, Pagination.sizeMax)
    dbRetriever.executeQuery[Disease, Query](diseaseQuery.query)
  }

  def getInteractions(ids: Seq[String],
                      scoreThreshold: Option[Double],
                      databaseName: Option[InteractionSourceEnum.Value],
                      pagination: Option[Pagination]
  ): Future[IndexedSeq[Interactions]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.interaction.name)
    val pag = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val interactionsQuery = OneToMany.interactionQuery(
      ids,
      tableName,
      scoreThreshold,
      databaseName,
      pag._1,
      pag._2
    )
    dbRetriever.executeQuery[Interactions, Query](interactionsQuery.query)
  }

  def getInteractionSources: Future[Seq[InteractionResources]] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.interaction.name)
    val interactionSourcesQuery = InteractionSourcesQuery(tableName)
    dbRetriever.executeQuery[InteractionResources, Query](interactionSourcesQuery.query)
  }

  def getEnhancerToGenes(chromosome: String,
                         start: Int,
                         end: Int,
                         pagination: Option[Pagination]
  ): Future[EnhancerToGenes] = {
    val tableName = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.enhancerToGene.name)
    val page = pagination.getOrElse(Pagination.mkDefault).offsetLimit
    val e2gQuery = EnhancerToGeneQuery(
      chromosome,
      start,
      end,
      tableName,
      page._1,
      page._2
    )
    val results =
      dbRetriever
        .executeQuery[EnhancerToGene, Query](e2gQuery.query)
        .map { e2g =>
          if (e2g.length > 0) then {
            EnhancerToGenes(e2g.head.meta_total, e2g)
          } else {
            EnhancerToGenes.empty
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

    logger.debug(s"querying credible sets",
                 keyValue("id", fixedEntityId),
                 keyValue("table", tableName)
    )

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
    logger.debug(s"querying associations with fixed disease",
                 keyValue("disease_id", disease.name),
                 keyValue("indirect", indirect)
    )
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
    logger.debug(s"querying associations with fixed target",
                 keyValue("target_id", target.approvedSymbol),
                 keyValue("indirect", indirect)
    )
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
    logger.debug(s"querying similarities",
                 keyValue("table", table),
                 keyValue("label", label),
                 keyValue("labels", labels)
    )

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

  def getLiteratureOcurrences(ids: Set[String],
                              startYear: Option[Int],
                              startMonth: Option[Int],
                              endYear: Option[Int],
                              endMonth: Option[Int],
                              cursor: Option[String]
  ): Future[Publications] = {
    val table = getTableWithPrefixOrDefault(defaultOTSettings.clickhouse.literatureIndex.name)
    val pag = Helpers.Cursor
      .to(cursor)
      .flatMap(_.asOpt[Pagination])
      .getOrElse(Pagination.mkDefault)
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
    val litQuery =
      LiteratureQuery(ids, table, filterStartDate, filterEndDate, pag.offset, pag.size)
    dbRetriever.executeQuery[Publications, Query](litQuery.query).map { v =>
      val pubs = v.head
      val nCursor = if (pubs.filteredCount < pag.size) {
        None
      } else {
        val npag = pag.next
        Helpers.Cursor.from(Some(Json.toJson(npag)))
      }
      pubs.copy(cursor = nCursor)
    }
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
