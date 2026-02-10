package models.gql

import models.entities.{
  Biosample,
  CredibleSet,
  Disease,
  Drug,
  Expressions,
  GeneOntologyTerm,
  HPO,
  Indications,
  MousePhenotypes,
  OtarProjects,
  SequenceOntologyTerm,
  Study,
  Target,
  TargetEssentiality,
  VariantIndex
}
import models.{Backend, entities}
import play.api.Logging
import sangria.execution.deferred.{Fetcher, FetcherCache, FetcherConfig, HasId, SimpleFetcherCache}

import scala.concurrent.*

object Fetchers extends Logging {
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

  def resetCache(): Unit = {
    logger.info("Clearing all GraphQL caches.")
    val fetchers: List[SimpleFetcherCache] = List(
      biosamplesFetcherCache,
      credibleSetFetcherCache,
      studyFetcherCache,
      hpoFetcherCache,
      goFetcherCache,
      variantFetcherCache,
      targetsFetcherCache,
      drugsFetcherCache,
      diseasesFetcherCache,
      indicationFetcherCache,
      mousePhenotypesFetcherCache,
      expressionFetcherCache,
      otarProjectsFetcherCache,
      soTermsFetcherCache,
      targetEssentialityFetcherCache
    )
    fetchers.foreach(_.clear())
  }
}
