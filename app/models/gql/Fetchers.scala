package models.gql

import models.entities.{
  Biosample,
  ClinicalReport,
  CredibleSet,
  Disease,
  Drug,
  DrugWarnings,
  Expressions,
  GeneOntologyTerm,
  HPO,
  MechanismsOfAction,
  MousePhenotypes,
  OtarProjects,
  PharmacogenomicsByDrug,
  PharmacogenomicsByTarget,
  PharmacogenomicsByVariant,
  SequenceOntologyTerm,
  Study,
  Target,
  TargetEssentiality,
  TargetPrioritisation,
  VariantIndex
}
import models.entities.Configuration.CacheSettings
import models.{Backend, entities}
import sangria.execution.deferred.{Fetcher, FetcherCache, FetcherConfig, HasId, SimpleFetcherCache}
import utils.OTLogging

import scala.concurrent.*

object Fetchers extends OTLogging {

  private var maxBytes: Long = 256L * 1024 * 1024

  def configure(settings: CacheSettings): Unit =
    maxBytes = settings.fetcherMaxMb * 1024 * 1024

  lazy val fetcherCache: LruFetcherCache = LruFetcherCache(maxBytes)

  implicit val soTermHasId: HasId[SequenceOntologyTerm, String] =
    HasId[SequenceOntologyTerm, String](_.id)
  val soTermsFetcher: Fetcher[Backend, SequenceOntologyTerm, SequenceOntologyTerm, String] =
    Fetcher(
      config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(fetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getSoTerms(ids)
    )

  // target
  implicit val targetHasId: HasId[Target, String] = HasId[Target, String](_.id)
  val targetsFetcher: Fetcher[Backend, Target, Target, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getTargets(ids)
  )

  // target essentiality
  implicit val targetEssentialityHasId: HasId[TargetEssentiality, String] =
    HasId[TargetEssentiality, String](_.id)
  val targetEssentialityFetcher: Fetcher[Backend, TargetEssentiality, TargetEssentiality, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(fetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getTargetEssentiality(ids)
    )

  // disease
  implicit val diseaseHasId: HasId[Disease, String] = HasId[Disease, String](_.id)
  val diseasesFetcher: Fetcher[Backend, Disease, Disease, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getDiseases(ids)
  )

  implicit val expressionHasId: HasId[Expressions, String] = HasId[Expressions, String](_.id)
  val expressionFetcher: Fetcher[Backend, Expressions, Expressions, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getExpressions(ids)
  )

  implicit val mechanismsOfActionHasId: HasId[MechanismsOfAction, String] =
    HasId[MechanismsOfAction, String](_.chemblId)
  val mechanismsOfActionFetcher: Fetcher[Backend, MechanismsOfAction, MechanismsOfAction, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(fetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getMechanismsOfAction(ids)
    )

  implicit val mousePhenotypesHasId: HasId[MousePhenotypes, String] =
    HasId[MousePhenotypes, String](_.id)
  val mousePhenotypesFetcher: Fetcher[Backend, MousePhenotypes, MousePhenotypes, String] = Fetcher(
    config = FetcherConfig
      .maxBatchSize(entities.Configuration.batchSize)
      .caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getMousePhenotypes(ids)
  )

  implicit val otarProjectsHasId: HasId[OtarProjects, String] = HasId[OtarProjects, String](_.efoId)
  val otarProjectsFetcher: Fetcher[Backend, OtarProjects, OtarProjects, String] = Fetcher(
    config = FetcherConfig
      .maxBatchSize(entities.Configuration.batchSize)
      .caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getOtarProjects(ids)
  )

  // hpo fetcher
  implicit val biosampleHasId: HasId[Biosample, String] = HasId[Biosample, String](_.biosampleId)
  val biosamplesFetcher: Fetcher[Backend, Biosample, Biosample, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getBiosamples(ids)
  )

  // hpo fetcher
  implicit val hpoHasId: HasId[HPO, String] = HasId[HPO, String](_.id)

  val hposFetcher: Fetcher[Backend, HPO, HPO, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getHPOs(ids)
  )

  // drug
  implicit val drugHasId: HasId[Drug, String] = HasId[Drug, String](_.id)

  val drugsFetcher: Fetcher[Backend, Drug, Drug, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getDrugs(ids)
  )

  implicit val drugWarningsHasId: HasId[DrugWarnings, String] =
    HasId[DrugWarnings, String](_.chemblId)
  val drugWarningsFetcher: Fetcher[Backend, DrugWarnings, DrugWarnings, String] = Fetcher(
    config = FetcherConfig
      .maxBatchSize(entities.Configuration.batchSize)
      .caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getDrugWarnings(ids)
  )

  implicit val goFetcherId: HasId[GeneOntologyTerm, String] = HasId[GeneOntologyTerm, String](_.id)
  val goFetcher: Fetcher[Backend, GeneOntologyTerm, GeneOntologyTerm, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getGoTerms(ids)
  )

  implicit val variantFetcherId: HasId[VariantIndex, String] =
    HasId[VariantIndex, String](_.variantId)
  val variantFetcher: Fetcher[Backend, VariantIndex, VariantIndex, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(fetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getVariants(ids)
  )

  val credibleSetFetcher: Fetcher[Backend, CredibleSet, CredibleSet, String] = {
    implicit val credibleSetFetcherId: HasId[CredibleSet, String] =
      HasId[CredibleSet, String](js => js.studyLocusId)
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(fetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getCredibleSet(ids)
    )
  }

  val studyFetcher: Fetcher[Backend, Study, Study, String] = {
    implicit val studyFetcherId: HasId[Study, String] =
      HasId[Study, String](js => (js.studyId))
    Fetcher(
      config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(fetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getStudy(ids)
    )
  }

  val clinicalReportFetcher: Fetcher[Backend, ClinicalReport, ClinicalReport, String] = {
    implicit val clinicalreportFetcherId: HasId[ClinicalReport, String] =
      HasId[ClinicalReport, String](js => js.id)
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(fetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getClinicalReports(ids)
    )
  }

  implicit val targetPrioritisationHasId: HasId[TargetPrioritisation, String] =
    HasId[TargetPrioritisation, String](_.targetId)
  val targetPrioritisationFetcher
      : Fetcher[Backend, TargetPrioritisation, TargetPrioritisation, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(fetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getTargetPrioritisation(ids)
    )

  implicit val pharmacogenomicsByDrugHasId: HasId[PharmacogenomicsByDrug, String] =
    HasId[PharmacogenomicsByDrug, String](js => js.drugId)
  val pharmacogenomicsByDrugFetcher
      : Fetcher[Backend, PharmacogenomicsByDrug, PharmacogenomicsByDrug, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(fetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getPharmacogenomicsByDrug(ids)
    )

  implicit val pharmacogenomicsByVariantHasId: HasId[PharmacogenomicsByVariant, String] =
    HasId[PharmacogenomicsByVariant, String](js => js.variantId)
  val pharmacogenomicsByVariantFetcher
      : Fetcher[Backend, PharmacogenomicsByVariant, PharmacogenomicsByVariant, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(fetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getPharmacogenomicsByVariant(ids)
    )

  implicit val pharmacogenomicsByTargetHasId: HasId[PharmacogenomicsByTarget, String] =
    HasId[PharmacogenomicsByTarget, String](js => js.targetFromSourceId)
  val pharmacogenomicsByTargetFetcher
      : Fetcher[Backend, PharmacogenomicsByTarget, PharmacogenomicsByTarget, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(fetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getPharmacogenomicsByTarget(ids)
    )

  def resetCache(): Unit = {
    logger.info("clearing GraphQL cache")
    fetcherCache.clear()
  }
  def cacheStats() =
    logger.info(
      s"Fetcher cache stats: ${fetcherCache.stats()}"
    )
}
