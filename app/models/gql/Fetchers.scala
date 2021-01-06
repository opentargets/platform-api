package models.gql

import models.Helpers.fromJsValue
import models.{Backend, entities}
import models.entities.{Disease, Drug, ECO, Expressions, Indications, MousePhenotypes, OtarProjects, Reactome, Target}
import play.api.libs.json.JsValue
import sangria.execution.deferred.{Fetcher, FetcherCache, FetcherConfig, HasId}

object Fetchers {
  def buildFetcher(index: String) = {
    implicit val soTermHasId = HasId[JsValue, String](el => (el \ "id").as[String])
    Fetcher(
      config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(soTermsFetcherCache),
      fetch = (ctx: Backend, ids: Seq[String]) => {
        val soIndexName = ctx.defaultESSettings.entities
          .find(_.name == index).map(_.index).getOrElse(index)

        ctx.esRetriever.getByIds(soIndexName, ids, fromJsValue[JsValue])
      })
  }

  val soTermsFetcherCache = FetcherCache.simple
  val soTermsFetcher = buildFetcher("so")

  // target
  implicit val targetHasId = HasId[Target, String](_.id)

  val targetsFetcherCache = FetcherCache.simple
  val targetsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(targetsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getTargets(ids)
    })

  // disease
  implicit val diseaseHasId = HasId[Disease, String](_.id)

  val diseasesFetcherCache = FetcherCache.simple
  val diseasesFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(diseasesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDiseases(ids)
    })

  implicit val expressionHasId = HasId[Expressions, String](_.id)

  val expressionFetcherCache = FetcherCache.simple
  val expressionFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(expressionFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getExpressions(ids)
    })


  implicit val otarProjectsHasId = HasId[OtarProjects, String](_.efoId)

  val otarProjectsFetcherCache = FetcherCache.simple
  val otarProjectsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(otarProjectsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getOtarProjects(ids)
    })


  implicit val mousePhenotypeHasId = HasId[MousePhenotypes, String](_.id)

  val mousePhenotypeFetcherCache = FetcherCache.simple
  val mousePhenotypeFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(mousePhenotypeFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getMousePhenotypes(ids)
    })

  implicit val reactomeHasId = HasId[Reactome, String](_.id)

  val reactomeFetcherCache = FetcherCache.simple
  val reactomeFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(reactomeFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getReactomeNodes(ids)
    })

  implicit val ecoHasId = HasId[ECO, String](_.id)

  val ecosFetcherCache = FetcherCache.simple
  val ecosFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(diseasesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getECOs(ids)
    })

  // drug
  implicit val drugHasId = HasId[Drug, String](_.id)

  val drugsFetcherCache = FetcherCache.simple

  val drugsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(drugsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDrugs(ids)
    })

  implicit val indicationHasId = HasId[Indications, String](_.id)
  val indicationFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getIndications(ids)
    })
}
