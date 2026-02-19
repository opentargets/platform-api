package models.gql

import models.entities.{
  Biosample,
  CredibleSet,
  Disease,
  Drug,
  DrugWarnings,
  Expressions,
  GeneOntologyTerm,
  HPO,
  Indications,
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
import models.{Backend, entities}
import sangria.execution.deferred.{Fetcher, FetcherCache, FetcherConfig, HasId, SimpleFetcherCache}
import utils.OTLogging

import scala.concurrent.*

object Fetchers extends OTLogging {

  val soTermsFetcherCache = FetcherCache.simple
  implicit val soTermHasId: HasId[SequenceOntologyTerm, String] =
    HasId[SequenceOntologyTerm, String](_.id)
  val soTermsFetcher: Fetcher[Backend, SequenceOntologyTerm, SequenceOntologyTerm, String] =
    Fetcher(
      config =
        FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(soTermsFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getSoTerms(ids)
    )
  val targetsFetcherCache = FetcherCache.simple

  // target
  implicit val targetHasId: HasId[Target, String] = HasId[Target, String](_.id)
  val targetsFetcher: Fetcher[Backend, Target, Target, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(targetsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getTargets(ids)
  )

  // target essentiality
  implicit val targetEssentialityHasId: HasId[TargetEssentiality, String] =
    HasId[TargetEssentiality, String](_.id)
  val targetEssentialityFetcherCache = FetcherCache.simple
  val targetEssentialityFetcher: Fetcher[Backend, TargetEssentiality, TargetEssentiality, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(targetEssentialityFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getTargetEssentiality(ids)
    )

  // disease
  val diseasesFetcherCache = FetcherCache.simple
  implicit val diseaseHasId: HasId[Disease, String] = HasId[Disease, String](_.id)
  val diseasesFetcher: Fetcher[Backend, Disease, Disease, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(diseasesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getDiseases(ids)
  )

  val expressionFetcherCache = FetcherCache.simple
  implicit val expressionHasId: HasId[Expressions, String] = HasId[Expressions, String](_.id)
  val expressionFetcher: Fetcher[Backend, Expressions, Expressions, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(expressionFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getExpressions(ids)
  )

  val mechanismsOfActionFetcherCache = FetcherCache.simple
  implicit val mechanismsOfActionHasId: HasId[MechanismsOfAction, String] =
    HasId[MechanismsOfAction, String](_.chemblId)
  val mechanismsOfActionFetcher: Fetcher[Backend, MechanismsOfAction, MechanismsOfAction, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(mechanismsOfActionFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getMechanismsOfAction(ids)
    )

  val mousePhenotypesFetcherCache = FetcherCache.simple
  implicit val mousePhenotypesHasId: HasId[MousePhenotypes, String] =
    HasId[MousePhenotypes, String](_.id)
  val mousePhenotypesFetcher: Fetcher[Backend, MousePhenotypes, MousePhenotypes, String] = Fetcher(
    config = FetcherConfig
      .maxBatchSize(entities.Configuration.batchSize)
      .caching(mousePhenotypesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getMousePhenotypes(ids)
  )

  val otarProjectsFetcherCache = FetcherCache.simple
  implicit val otarProjectsHasId: HasId[OtarProjects, String] = HasId[OtarProjects, String](_.efoId)
  val otarProjectsFetcher: Fetcher[Backend, OtarProjects, OtarProjects, String] = Fetcher(
    config = FetcherConfig
      .maxBatchSize(entities.Configuration.batchSize)
      .caching(otarProjectsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getOtarProjects(ids)
  )

  // hpo fetcher
  implicit val biosampleHasId: HasId[Biosample, String] = HasId[Biosample, String](_.biosampleId)
  val biosamplesFetcherCache = FetcherCache.simple
  val biosamplesFetcher: Fetcher[Backend, Biosample, Biosample, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(biosamplesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getBiosamples(ids)
  )

  // hpo fetcher
  implicit val hpoHasId: HasId[HPO, String] = HasId[HPO, String](_.id)

  val hpoFetcherCache = FetcherCache.simple
  val hposFetcher: Fetcher[Backend, HPO, HPO, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(hpoFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getHPOs(ids)
  )

  // drug
  implicit val drugHasId: HasId[Drug, String] = HasId[Drug, String](_.id)

  val drugsFetcherCache = FetcherCache.simple
  val drugsFetcher: Fetcher[Backend, Drug, Drug, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(drugsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getDrugs(ids)
  )

  val drugWarningsFetcherCache = FetcherCache.simple
  implicit val drugWarningsHasId: HasId[DrugWarnings, String] =
    HasId[DrugWarnings, String](_.chemblId)
  val drugWarningsFetcher: Fetcher[Backend, DrugWarnings, DrugWarnings, String] = Fetcher(
    config = FetcherConfig
      .maxBatchSize(entities.Configuration.batchSize)
      .caching(drugWarningsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getDrugWarnings(ids)
  )

  implicit val indicationHasId: HasId[Indications, String] = HasId[Indications, String](_.id)
  val indicationFetcherCache = FetcherCache.simple
  val indicationFetcher: Fetcher[Backend, Indications, Indications, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getIndications(ids)
  )

  implicit val goFetcherId: HasId[GeneOntologyTerm, String] = HasId[GeneOntologyTerm, String](_.id)
  val goFetcherCache = FetcherCache.simple
  val goFetcher: Fetcher[Backend, GeneOntologyTerm, GeneOntologyTerm, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(goFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getGoTerms(ids)
  )

  implicit val variantFetcherId: HasId[VariantIndex, String] =
    HasId[VariantIndex, String](_.variantId)
  val variantFetcherCache = FetcherCache.simple
  val variantFetcher: Fetcher[Backend, VariantIndex, VariantIndex, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(variantFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => ctx.getVariants(ids)
  )

  val credibleSetFetcherCache = FetcherCache.simple
  val credibleSetFetcher: Fetcher[Backend, CredibleSet, CredibleSet, String] = {
    implicit val credibleSetFetcherId: HasId[CredibleSet, String] =
      HasId[CredibleSet, String](js => js.studyLocusId)
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(credibleSetFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getCredibleSet(ids)
    )
  }

  val studyFetcherCache = FetcherCache.simple
  val studyFetcher: Fetcher[Backend, Study, Study, String] = {
    implicit val studyFetcherId: HasId[Study, String] =
      HasId[Study, String](js => (js.studyId))
    Fetcher(
      config =
        FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(studyFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getStudy(ids)
    )
  }

  val targetPrioritisationFetcherCache = FetcherCache.simple
  implicit val targetPrioritisationHasId: HasId[TargetPrioritisation, String] =
    HasId[TargetPrioritisation, String](_.targetId)
  val targetPrioritisationFetcher
      : Fetcher[Backend, TargetPrioritisation, TargetPrioritisation, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(targetPrioritisationFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getTargetPrioritisation(ids)
    )

  val pharmacogenomicsByDrugFetcherCache = FetcherCache.simple
  implicit val pharmacogenomicsByDrugHasId: HasId[PharmacogenomicsByDrug, String] =
    HasId[PharmacogenomicsByDrug, String](js => js.drugId)
  val pharmacogenomicsByDrugFetcher
      : Fetcher[Backend, PharmacogenomicsByDrug, PharmacogenomicsByDrug, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(pharmacogenomicsByDrugFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getPharmacogenomicsByDrug(ids)
    )

  val pharmacogenomicsByVariantFetcherCache = FetcherCache.simple
  implicit val pharmacogenomicsByVariantHasId: HasId[PharmacogenomicsByVariant, String] =
    HasId[PharmacogenomicsByVariant, String](js => js.variantId)
  val pharmacogenomicsByVariantFetcher
      : Fetcher[Backend, PharmacogenomicsByVariant, PharmacogenomicsByVariant, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(pharmacogenomicsByVariantFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getPharmacogenomicsByVariant(ids)
    )

  val pharmacogenomicsByTargetFetcherCache = FetcherCache.simple
  implicit val pharmacogenomicsByTargetHasId: HasId[PharmacogenomicsByTarget, String] =
    HasId[PharmacogenomicsByTarget, String](js => js.targetFromSourceId)
  val pharmacogenomicsByTargetFetcher
      : Fetcher[Backend, PharmacogenomicsByTarget, PharmacogenomicsByTarget, String] =
    Fetcher(
      config = FetcherConfig
        .maxBatchSize(entities.Configuration.batchSize)
        .caching(pharmacogenomicsByTargetFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => ctx.getPharmacogenomicsByTarget(ids)
    )

  def resetCache(): Unit = {
    logger.info("clearing all GraphQL caches")
    val fetchers: List[SimpleFetcherCache] = List(
      biosamplesFetcherCache,
      credibleSetFetcherCache,
      studyFetcherCache,
      hpoFetcherCache,
      goFetcherCache,
      variantFetcherCache,
      targetsFetcherCache,
      drugsFetcherCache,
      drugWarningsFetcherCache,
      diseasesFetcherCache,
      indicationFetcherCache,
      mechanismsOfActionFetcherCache,
      mousePhenotypesFetcherCache,
      expressionFetcherCache,
      otarProjectsFetcherCache,
      pharmacogenomicsByDrugFetcherCache,
      pharmacogenomicsByVariantFetcherCache,
      pharmacogenomicsByTargetFetcherCache,
      soTermsFetcherCache,
      targetEssentialityFetcherCache,
      targetPrioritisationFetcherCache
    )
    fetchers.foreach(_.clear())
  }
}
