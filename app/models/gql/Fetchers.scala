package models.gql

import models.Helpers.fromJsValue
import models.entities.{Disease, Drug, ECO, Expressions, GeneOntologyTerm, HPO, Indications, MousePhenotype, MousePhenotypes, OtarProjects, Reactome, Target, GeneOntology => GO}
import models.{Backend, entities}
import play.api.Logging
import play.api.libs.json.JsValue
import sangria.execution.deferred.{Fetcher, FetcherCache, FetcherConfig, HasId, SimpleFetcherCache}

object Fetchers extends Logging {
  val soTermsFetcherCache = FetcherCache.simple
  val soTermsFetcher = buildFetcher("so")
  val targetsFetcherCache = FetcherCache.simple

  // target
  implicit val targetHasId = HasId[Target, String](_.id)
  val targetsFetcher = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(targetsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getTargets(ids)
    }
  )
  val diseasesFetcherCache = FetcherCache.simple

  // disease
  implicit val diseaseHasId = HasId[Disease, String](_.id)
  val diseasesFetcher = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(diseasesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDiseases(ids)
    }
  )
  val expressionFetcherCache = FetcherCache.simple

  implicit val expressionHasId = HasId[Expressions, String](_.id)
  val expressionFetcher = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(expressionFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getExpressions(ids)
    }
  )
  val otarProjectsFetcherCache = FetcherCache.simple

  implicit val otarProjectsHasId = HasId[OtarProjects, String](_.efoId)
  val otarProjectsFetcher = Fetcher(
    config = FetcherConfig
      .maxBatchSize(entities.Configuration.batchSize)
      .caching(otarProjectsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getOtarProjects(ids)
    }
  )

  val reactomeFetcherCache = FetcherCache.simple

  implicit val reactomeHasId = HasId[Reactome, String](_.id)
  val reactomeFetcher = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(reactomeFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getReactomeNodes(ids)
    }
  )
  val ecosFetcherCache = FetcherCache.simple

  implicit val ecoHasId = HasId[ECO, String](_.id)
  val ecosFetcher = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(diseasesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getECOs(ids)
    }
  )

  //hpo fetcher
  implicit val hpoHasId = HasId[HPO, String](_.id)

  val hpoFetcherCache = FetcherCache.simple
  val hposFetcher = Fetcher(
    config =
      FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(hpoFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getHPOs(ids)
    }
  )

  // drug
  implicit val drugHasId = HasId[Drug, String](_.id)

  val drugsFetcherCache = FetcherCache.simple
  val drugsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(drugsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDrugs(ids)
    }
  )

  implicit val indicationHasId = HasId[Indications, String](_.id)
  val indicationFetcher = Fetcher(config =
    FetcherConfig.maxBatchSize(entities.Configuration.batchSize),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getIndications(ids)
    })

  implicit val goFetcherId = HasId[GeneOntologyTerm, String](_.id)
  val goFetcherCache = FetcherCache.simple
  val goFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(goFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getGoTerms(ids)
    }
  )

  def buildFetcher(index: String) = {
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
      ecosFetcherCache,
      reactomeFetcherCache,
      expressionFetcherCache,
      otarProjectsFetcherCache,
      soTermsFetcherCache
    )
    fetchers.foreach(_.clear())
  }
}
