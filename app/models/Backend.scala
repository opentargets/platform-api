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
import models.db.{QAOTF, QLITAGG, QW2V, IntervalsQuery}
import models.entities.Publication.*
import models.entities.Associations.*
import models.entities.Biosample.*
import models.entities.CredibleSet.*
import models.entities.Configuration.*
import models.entities.DiseaseHPOs.*
import models.entities.Drug.*
import models.entities.Intervals.*
import models.entities.Loci.*
import models.entities.MousePhenotypes.*
import models.entities.Pharmacogenomics.*
import models.entities.ProteinCodingCoordinates.*
import models.entities.SearchFacetsResults.*
import models.entities.Evidence.*
import models.entities.SequenceOntologyTerm.*
import models.entities.*
import models.gql.StudyTypeEnum
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
    val indexName = getIndexOrDefault("l2g_predictions")
    val pag = pagination.getOrElse(Pagination.mkDefault)
    val queries = ids.map { studyLocusId =>
      IndexQuery(
        esIndex = indexName,
        kv = Map("studyLocusId.keyword" -> Seq(studyLocusId)),
        filters = Seq.empty,
        pagination = pag
      )
    }
    val retriever =
      esRetriever
        .getMultiByIndexedTermsMust(
          queries,
          fromJsValue[L2GPrediction],
          ElasticRetriever.sortBy("score", SortOrder.Desc),
          Some(ResolverField("studyLocusId"))
        )
    retriever.map { case r =>
      r.map {
        case Results(Seq(), _, _, _) => L2GPredictions.empty
        case Results(predictions, _, counts, studyLocusId) =>
          L2GPredictions(counts, predictions, studyLocusId.as[String])
      }
    }
  }

  def getVariants(ids: Seq[String]): Future[IndexedSeq[VariantIndex]] = {
    val indexName = getIndexOrDefault("variant")
    val r = esRetriever
      .getByIndexedTermsMust(indexName,
                             Map("variantId.keyword" -> ids),
                             Pagination.mkMax,
                             fromJsValue[VariantIndex]
      )
      .map(_.mappedHits)
    r
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
    val indexName = getIndexOrDefault("study")
    val termsQuery = Map("studyId.keyword" -> ids)
    val retriever =
      esRetriever
        .getByIndexedTermsMust(
          indexName,
          termsQuery,
          Pagination.mkMax,
          fromJsValue[Study]
        )
    retriever.map(_.mappedHits)
  }

  def getStudies(queryArgs: StudyQueryArgs, pagination: Option[Pagination]): Future[Studies] = {
    val pag = pagination.getOrElse(Pagination.mkDefault)
    val indexName = getIndexOrDefault("study")
    val diseaseIds: Seq[String] =
      if (queryArgs.enableIndirect) {
        val diseases = getDiseases(queryArgs.diseaseIds)
        val descendantEfos = diseases.map(_.map(_.descendants).flatten).await
        descendantEfos ++: queryArgs.diseaseIds
      } else {
        queryArgs.diseaseIds
      }
    val termsQuery = Map(
      "studyId.keyword" -> queryArgs.id,
      "traitFromSourceMappedIds.keyword" -> diseaseIds
    ).filter(_._2.nonEmpty)
    if (termsQuery.isEmpty) {
      Future.successful(Studies.empty)
    } else {
      val retriever = esRetriever
        .getByIndexedTermsMust(
          indexName,
          termsQuery,
          pag,
          fromJsValue[Study]
        )
      retriever.map {
        case Results(Seq(), _, _, _) => Studies.empty
        case Results(studies, _, count, _) =>
          Studies(count, studies)
      }
    }
  }

  def getColocalisations(studyLocusIds: Seq[String],
                         studyTypes: Option[Seq[StudyTypeEnum.Value]],
                         pagination: Option[Pagination]
  ): Future[IndexedSeq[Colocalisations]] = {
    val indexName = getIndexOrDefault("colocalisation")
    val pag = pagination.getOrElse(Pagination.mkDefault)
    val boolQueries: Seq[IndexBoolQuery] = studyLocusIds.map { studyLocusId =>
      val leftStudyLocusQuery = must(
        termQuery("leftStudyLocusId.keyword", studyLocusId),
        termsQuery("rightStudyType.keyword", studyTypes.getOrElse(StudyTypeEnum.values))
      )
      // Get the coloc based on the right study locus only if the other (left) study type is gwas
      // because left study locus is always gwas and the field is not represented.
      val rightStudyLocusQuery = studyTypes match {
        case Some(st) =>
          if (st.contains(StudyTypeEnum.gwas)) {
            Some(termQuery("rightStudyLocusId.keyword", studyLocusId))
          } else {
            None
          }
        case None => Some(termQuery("rightStudyLocusId.keyword", studyLocusId))
      }
      val query: BoolQuery = {
        rightStudyLocusQuery match {
          case Some(rq) =>
            should(leftStudyLocusQuery, rq)
          case None => must(leftStudyLocusQuery)
        }
      }.queryName(studyLocusId)
      IndexBoolQuery(
        esIndex = indexName,
        boolQuery = query,
        pagination = pag
      )
    }
    val retriever =
      esRetriever
        .getMultiQ(
          boolQueries,
          fromJsValue[Colocalisation],
          None,
          Some(ResolverField(matched_queries = true))
        )
    retriever.map { case r =>
      r.map {
        case Results(Seq(), _, _, _) => Colocalisations.empty
        case Results(colocs, _, counts, studyLocusId) =>
          val idString = studyLocusId.as[String]
          val c = colocs.map { coloc =>
            if (coloc.leftStudyLocusId == idString) {
              coloc.copy(otherStudyLocusId = Some(coloc.rightStudyLocusId))
            } else {
              coloc.copy(otherStudyLocusId = Some(coloc.leftStudyLocusId))
            }
          }
          Colocalisations(counts, c, idString)
      }
    }
  }

  def getLocus(studyLocusIds: Seq[String],
               variantIds: Option[Seq[String]],
               pagination: Option[Pagination]
  ): Future[IndexedSeq[Loci]] = {
    val indexName = getIndexOrDefault("credible_set")
    val limitClause = pagination.getOrElse(Pagination.mkDefault).toES
    val termsQuerySeq = Seq(Map("studyLocusId.keyword" -> studyLocusIds))
    val termsQueryIter = termsQuerySeq.map { termsQuerySeq =>
      Iterable(must(termsQuerySeq.map { it =>
        val terms = it._2.asInstanceOf[Iterable[String]]
        termsQuery(it._1, terms)
      }))
    }
    def nestedQueryBuilder(path: String, query: BoolQuery) =
      nestedQuery(path, query).inner(innerHits("locus").size(limitClause._2).from(limitClause._1))
    val nestedQueryIter = variantIds match {
      case Some(variantIds) =>
        Iterable(
          nestedQueryBuilder("locus", must(termsQuery("locus.variantId.keyword", variantIds)))
        )
      case None =>
        Iterable(
          nestedQueryBuilder("locus",
                             must(
                               matchAllQuery()
                             )
          )
        )

    }
    val query: BoolQuery = must(termsQueryIter.flatten ++ nestedQueryIter)
    val retriever =
      esRetriever
        .getInnerQ(
          indexName,
          query,
          Pagination.mkMax,
          fromJsValue[Locus],
          "locus",
          Some("studyLocusId")
        )
    retriever.map { case InnerResults(locus, _, counts, studyLocusIds) =>
      locus.zip(counts).zip(studyLocusIds).map { case ((locus, count), studyLocusId) =>
        Loci(count, Some(locus), studyLocusId.as[String])
      }
    }
  }

  def getCredibleSet(ids: Seq[String]): Future[IndexedSeq[CredibleSet]] = {
    val indexName = getIndexOrDefault("credible_set")
    val termsQuery = Map("studyLocusId.keyword" -> ids)
    val retriever =
      esRetriever
        .getByIndexedTermsMust(
          indexName,
          termsQuery,
          Pagination.mkMax,
          fromJsValue[CredibleSet],
          excludedFields = Seq("locus", "ldSet")
        )
    retriever.map(_.mappedHits)
  }

  def getCredibleSets(
      queryArgs: CredibleSetQueryArgs,
      pagination: Option[Pagination]
  ): Future[CredibleSets] = {
    val pag = pagination.getOrElse(Pagination.mkDefault)
    val indexName = getIndexOrDefault("credible_set")
    val termsQuerySeq = Map(
      "studyLocusId.keyword" -> queryArgs.ids,
      "studyId.keyword" -> queryArgs.studyIds,
      "studyType.keyword" -> queryArgs.studyTypes,
      "region.keyword" -> queryArgs.regions
    ).filter(_._2.nonEmpty).toSeq
    val termsQueryIter: Iterable[queries.Query] = Iterable(must(termsQuerySeq.map { it =>
      val terms = it._2.asInstanceOf[Iterable[String]]
      termsQuery(it._1, terms)
    }))
    val query: BoolQuery =
      if (queryArgs.variantIds.nonEmpty) {
        val nestedTermsQuery = Map("locus.variantId.keyword" -> queryArgs.variantIds)
        val nestedQueryIter = Iterable(
          nestedQuery("locus",
                      must(nestedTermsQuery.map { it =>
                        val terms = it._2.asInstanceOf[Iterable[String]]
                        termsQuery(it._1, terms)
                      })
          )
        )
        must(termsQueryIter ++ nestedQueryIter)
      } else {
        must(termsQueryIter)
      }
    val retriever =
      esRetriever
        .getQ(
          indexName,
          query,
          pag,
          fromJsValue[CredibleSet],
          excludedFields = Seq("locus", "ldSet")
        )
    retriever.map {
      case Results(Seq(), _, _, _) => CredibleSets.empty
      case Results(credset, _, count, _) =>
        CredibleSets(count, credset)
    }
  }

  def getCredibleSetsByStudy(studyIds: Seq[String],
                             pagination: Option[Pagination]
  ): Future[IndexedSeq[CredibleSets]] = {
    val pag = pagination.getOrElse(Pagination.mkDefault)
    val indexName = getIndexOrDefault("credible_set")
    val queries = studyIds.map { studyId =>
      IndexQuery(
        esIndex = indexName,
        kv = Map("studyId.keyword" -> Seq(studyId)),
        filters = Seq.empty,
        pagination = pag,
        aggs = Seq.empty,
        excludedFields = Seq("locus", "ldSet")
      )
    }
    val retriever =
      esRetriever
        .getMultiByIndexedTermsMust(
          queries,
          fromJsValue[CredibleSet],
          None,
          Some(ResolverField("studyId"))
        )
    retriever.map { case r =>
      r.map {
        case Results(Seq(), _, _, _) => CredibleSets.empty
        case Results(credsets, _, counts, studyId) =>
          CredibleSets(counts, credsets, studyId.as[String])
      }
    }
  }

  def getCredibleSetsByVariant(variantIds: Seq[String],
                               studyTypes: Option[Seq[StudyTypeEnum.Value]],
                               pagination: Option[Pagination]
  ): Future[IndexedSeq[CredibleSets]] = {
    val pag = pagination.getOrElse(Pagination.mkDefault)
    val indexName = getIndexOrDefault("credible_set")
    val termsQueryIter: Option[Iterable[queries.Query]] = studyTypes match {
      case Some(studyTypes) => Some(Iterable(should(termsQuery("studyType.keyword", studyTypes))))
      case None             => None
    }
    // nested query for each variant id in variantIds
    val boolQueries: Seq[IndexBoolQuery] = variantIds.map { variantId =>
      val query: BoolQuery = {
        val nestedTermsQuery = termQuery("locus.variantId.keyword", variantId)
        val nestedQueryIter =
          Iterable(nestedQuery("locus", must(nestedTermsQuery)).inner(innerHits("locus").size(1)))
        termsQueryIter match {
          case None                 => must(nestedQueryIter)
          case Some(termsQueryIter) => must(termsQueryIter ++ nestedQueryIter)
        }
      }.queryName(variantId)
      IndexBoolQuery(
        esIndex = indexName,
        boolQuery = query,
        pagination = pag,
        excludedFields = Seq("ldSet", "locus")
      )
    }
    val retriever =
      esRetriever
        .getMultiQ(
          boolQueries,
          fromJsValue[CredibleSet],
          None,
          Some(ResolverField(matched_queries = true))
        )
    retriever.map { case r =>
      r.map {
        case Results(Seq(), _, _, _) => CredibleSets.empty
        case Results(credsets, _, counts, variantId) =>
          CredibleSets(counts, credsets, variantId.as[String])
      }
    }
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

    val filters: Map[String, Seq[String]] = Map(
      "variantId.keyword" -> Seq(variantId)
    )

    getFilteredEvidences(datasourceIds, filters, orderBy, sizeLimit, cursor)
  }

  def getEvidencesByEfoId(
      datasourceIds: Option[Seq[String]],
      targetIds: Seq[String],
      diseaseIds: Seq[String],
      orderBy: Option[(String, String)],
      sizeLimit: Option[Int],
      cursor: Option[String]
  ): Future[Evidences] = {

    val filters: Map[String, Seq[String]] = Map(
      "targetId.keyword" -> targetIds,
      "diseaseId.keyword" -> diseaseIds
    )

    getFilteredEvidences(datasourceIds, filters, orderBy, sizeLimit, cursor)
  }

  // TODO CHECK RESULTS ARE SIZE 0 OR OPTIMISE FIELDS TO BRING BACK

  /** get evidences by multiple parameters */
  private def getFilteredEvidences(
      datasourceIds: Option[Seq[String]],
      filters: Map[String, Seq[String]],
      orderBy: Option[(String, String)],
      sizeLimit: Option[Int],
      cursor: Option[String]
  ): Future[Evidences] = {

    val pag = sizeLimit.getOrElse(Pagination.sizeDefault)
    val sortByField = orderBy.flatMap { p =>
      ElasticRetriever.sortBy(p._1, if (p._2 == "desc") SortOrder.Desc else SortOrder.Asc)
    }

    val cbIndexPrefix = getIndexOrDefault("evidences", Some("evidence_"))

    val cbIndex = datasourceIds
      .map(_.map(cbIndexPrefix.concat).mkString(","))
      .getOrElse(cbIndexPrefix.concat("*"))

    esRetriever
      .getByMustWithSearch(
        cbIndex,
        filters,
        pag,
        fromJsValue[Evidence],
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
    val targetIndexName = getIndexOrDefault("target", Some("targets"))

    esRetriever.getByIds(targetIndexName, ids, fromJsValue[Target])
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
    val diseaseIndexName = getIndexOrDefault("disease")
    esRetriever.getByIds(diseaseIndexName, ids, fromJsValue[Disease])
  }

  def getIntervals(chromosome: String,
                   start: Int,
                   end: Int,
                   pagination: Option[Pagination]
  ): Future[Intervals] = {
    val tableName = defaultOTSettings.clickhouse.intervals.name
    val page = pagination.getOrElse(Pagination.mkDefault)
    val intervalsQuery = IntervalsQuery(
      chromosome,
      start,
      end,
      tableName,
      page.index,
      page.size
    )
    val total: Int = dbRetriever
      .executeQuery[Int, Query](intervalsQuery.totals)
      .map {
        case Seq(totalCount) => totalCount
        case _               => 0
      }
      .await
    logger.info(s"Total intervals found: $total")

    val results =
      if total == 0 then Future.successful(Intervals(total, Vector.empty))
      else
        dbRetriever
          .executeQuery[Interval, Query](intervalsQuery.query)
          .map(intervals => Intervals(total, intervals))
    results
  }

  def mapIds(
      queryTerms: Seq[String],
      entityNames: Seq[String]
  ): Future[MappingResults] = {

    val entities = for {
      e <- defaultESSettings.entities
      if (entityNames.contains(e.name) && e.searchIndex.isDefined)
    } yield e
    esRetriever.getTermsResultsMapping(entities, queryTerms)
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
      entityNames: Seq[String],
      category: Option[String]
  ): Future[SearchFacetsResults] = {
    val entities = for {
      e <- defaultESSettings.entities
      if (entityNames.contains(e.name) && e.facetSearchIndex.isDefined)
    } yield e
    esRetriever.getSearchFacetsResultSet(entities,
                                         qString,
                                         pagination.getOrElse(Pagination.mkDefault),
                                         category
    )
  }

  def getAssociationDatasources: Future[Vector[EvidenceSource]] =
    dbRetriever.getUniqList[EvidenceSource](
      Seq("datasource_id", "datatype_id"),
      defaultOTSettings.clickhouse.disease.associations.name
    )

  def getAssociationsEntityFixed(
      tableName: String,
      datasources: Option[Seq[DatasourceSettings]],
      fixedEntityId: String,
      indirectIds: Set[String],
      bIds: Set[String],
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
      defaultOTSettings.clickhouse.disease.associations.name,
      datasources,
      disease.id,
      indirectIDs,
      targetIds,
      filter,
      orderBy,
      pagination
    )
  }

  def getAssociationsTargetFixed(
      target: Target,
      datasources: Option[Seq[DatasourceSettings]],
      indirect: Boolean,
      facetFilters: Seq[String],
      diseaseSet: Set[String],
      filter: Option[String],
      orderBy: Option[(String, String)],
      pagination: Option[Pagination]
  ): Future[Associations] = {
    logger.debug(s"get target id ${target.approvedSymbol} ACTUALLY DISABLED!")
    val indirectIDs = if (indirect) {
      val interactions =
        Interactions.find(target.id, None, None, pagination = Some(Pagination(0, 10000))) map {
          case Some(ints) =>
            ints.rows
              .flatMap(int => int.targetB.filter(_.startsWith("ENSG")))
              .toSet + target.id
          case None => Set.empty + target.id
        }
      interactions.await
    } else Set.empty[String]

    val diseaseIds =
      applyFacetFiltersToBIDs("facet_search_disease", diseaseSet, facetFilters)

    getAssociationsEntityFixed(
      defaultOTSettings.clickhouse.target.associations.name,
      datasources,
      target.id,
      indirectIDs,
      diseaseIds,
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
    val table = defaultOTSettings.clickhouse.similarities
    logger.debug(s"query similarities in table ${table.name}")

    val jointLabels = labels + label
    val simQ = QW2V(table.name, categories, jointLabels, threshold, size)
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
    val table = defaultOTSettings.clickhouse.literature
    val indexTable = defaultOTSettings.clickhouse.literatureIndex
    logger.debug(s"query literature ocurrences in table ${table.name}")

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

    val simQ = QLITAGG(table.name,
                       indexTable.name,
                       ids,
                       pag.size,
                       pag.offset,
                       filterStartDate,
                       filterEndDate
    )

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
    defaultESSettings.entities
      .find(_.name == index)
      .map(_.index)
      .getOrElse(default.getOrElse(index))

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
      val entityIdsFromFacets: Seq[Set[String]] =
        resolveEntityIdsFromFacets(facetFilters, index)
      val entityIdsFromFacetsIntersect: Set[String] =
        if (entityIdsFromFacets.isEmpty) Set.empty
        else entityIdsFromFacets.reduce(_ intersect _)
      if (bIDs.isEmpty) emptySetToSetOfEmptyString(entityIdsFromFacetsIntersect)
      else emptySetToSetOfEmptyString(bIDs.intersect(entityIdsFromFacetsIntersect))
    }
}
