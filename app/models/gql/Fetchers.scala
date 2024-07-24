package models.gql

import models.Helpers.fromJsValue
import models.entities.{CredibleSet, Disease, Drug, Expressions, GeneOntologyTerm, GwasIndex, HPO, Indications, OtarProjects, Reactome, Target, VariantIndex}
import models.{Backend, entities}
import play.api.Logging
import play.api.libs.json.{JsValue, __}
import sangria.execution.deferred.{Fetcher, FetcherCache, FetcherConfig, HasId, SimpleFetcherCache}

object Fetchers extends Logging {
  val soTermsFetcherCache = FetcherCache.simple
  val soTermsFetcher: Fetcher[Backend, JsValue, JsValue, String] = buildFetcher("so")
  val targetsFetcherCache = FetcherCache.simple

  // target
  implicit val targetHasId: HasId[Target, String] = HasId[Target, String](_.id)
  val targetsFetcher: Fetcher[Backend, Target, Target, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(targetsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getTargets(ids)
    }
  )
  val diseasesFetcherCache = FetcherCache.simple

  // disease
  implicit val diseaseHasId: HasId[Disease, String] = HasId[Disease, String](_.id)
  val diseasesFetcher: Fetcher[Backend, Disease, Disease, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(diseasesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDiseases(ids)
    }
  )
  val expressionFetcherCache = FetcherCache.simple

  implicit val expressionHasId: HasId[Expressions, String] = HasId[Expressions, String](_.id)
  val expressionFetcher: Fetcher[Backend, Expressions, Expressions, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(expressionFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getExpressions(ids)
    }
  )
  val otarProjectsFetcherCache = FetcherCache.simple

  implicit val otarProjectsHasId: HasId[OtarProjects, String] = HasId[OtarProjects, String](_.efoId)
  val otarProjectsFetcher: Fetcher[Backend, OtarProjects, OtarProjects, String] = Fetcher(
    config = FetcherConfig
      .maxBatchSize(entities.Configuration.batchSize)
      .caching(otarProjectsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getOtarProjects(ids)
    }
  )

  val reactomeFetcherCache = FetcherCache.simple

  implicit val reactomeHasId: HasId[Reactome, String] = HasId[Reactome, String](_.id)
  val reactomeFetcher: Fetcher[Backend, Reactome, Reactome, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(reactomeFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getReactomeNodes(ids)
    }
  )

  //hpo fetcher
  implicit val hpoHasId: HasId[HPO, String] = HasId[HPO, String](_.id)

  val hpoFetcherCache = FetcherCache.simple
  val hposFetcher: Fetcher[Backend, HPO, HPO, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(hpoFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getHPOs(ids)
    }
  )

  // drug
  implicit val drugHasId: HasId[Drug, String] = HasId[Drug, String](_.id)

  val drugsFetcherCache = FetcherCache.simple
  val drugsFetcher: Fetcher[Backend, Drug, Drug, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(drugsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDrugs(ids)
    }
  )

  implicit val indicationHasId: HasId[Indications, String] = HasId[Indications, String](_.id)
  val indicationFetcher: Fetcher[Backend, Indications, Indications, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getIndications(ids)
    }
  )

  implicit val goFetcherId: HasId[GeneOntologyTerm, String] = HasId[GeneOntologyTerm, String](_.id)
  val goFetcherCache = FetcherCache.simple
  val goFetcher: Fetcher[Backend, GeneOntologyTerm, GeneOntologyTerm, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(goFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getGoTerms(ids)
    }
  )

  implicit val variantFetcherId: HasId[VariantIndex, String] =
    HasId[VariantIndex, String](_.variantId)
  val variantFetcherCache = FetcherCache.simple
  val variantFetcher: Fetcher[Backend, VariantIndex, VariantIndex, String] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(variantFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getVariants(ids)
    }
  )

  implicit val credSetFetcherId: HasId[CredibleSet, Long] =
    HasId[CredibleSet, Long](_.studyLocusId)
  val credSetFetcherCache = FetcherCache.simple
  val credSetFetcher: Fetcher[Backend, CredibleSet, CredibleSet, Long] = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(credSetFetcherCache),
    fetch = (ctx: Backend, ids: Seq[Long]) => {
      ctx.getCredSet(ids)
    }
  )

  val gwasFetcherCache = FetcherCache.simple
  val gwasFetcher: Fetcher[Backend, JsValue, JsValue, String] = {
    implicit val gwasFetcherId: HasId[JsValue, String] =
      HasId[JsValue, String](js => (js \ "studyId").as[String])
    Fetcher(
      config =
        FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(gwasFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => {
        ctx.getGwasIndexes(ids)
      }
    )
  }

  def buildFetcher(index: String): Fetcher[Backend, JsValue, JsValue, String] = {
    implicit val soTermHasId = HasId[JsValue, String](el => (el \ "id").as[String])
    Fetcher(
      config =
        FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(soTermsFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => {
        val soIndexName = ctx.defaultESSettings.entities
          .find(_.name == index)
          .map(_.index)
          .getOrElse(index)

        ctx.esRetriever.getByIds(soIndexName, ids, fromJsValue[JsValue])
      }
    )
  }

  def resetCache(): Unit = {
    logger.info("Clearing all GraphQL caches.")
    val fetchers: List[SimpleFetcherCache] = List(
      targetsFetcherCache,
      drugsFetcherCache,
      diseasesFetcherCache,
      reactomeFetcherCache,
      expressionFetcherCache,
      otarProjectsFetcherCache,
      soTermsFetcherCache
    )
    fetchers.foreach(_.clear())
  }
}
