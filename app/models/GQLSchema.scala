package models

import play.api.libs.json.Json
import sangria.macros.derive._
import sangria.marshalling.playJson._
import sangria.schema._
import entities._
import Entities.JSONImplicits._
import sangria.execution.deferred._

trait GQLMeta {
  implicit val metaVersionImp = deriveObjectType[Backend, Entities.MetaVersion]()
  implicit val metaImp = deriveObjectType[Backend, Entities.Meta]()
}

trait GQLSearchResult {
  implicit val searchResultAggsCategoryImp = deriveObjectType[Backend, models.entities.SearchResultAggCategory]()
  implicit val searchResultAggsEntityImp = deriveObjectType[Backend, models.entities.SearchResultAggEntity]()
  implicit val searchResultAggsImp = deriveObjectType[Backend, models.entities.SearchResultAggs]()
  implicit val searchResultImp = deriveObjectType[Backend, models.entities.SearchResult]()
  implicit val searchResultsImp = deriveObjectType[Backend, models.entities.SearchResults]()
}

trait GQLTarget {
  implicit val targetHasId = HasId[Target, String](_.id)

  val targetsFetcherCache = FetcherCache.simple
  val targetsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(100).caching(targetsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getTargets(ids)
    })

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit val proteinImp = deriveObjectType[Backend, Protein]()
  implicit val genomicLocationImp = deriveObjectType[Backend, GenomicLocation]()
  implicit val targetImp = deriveObjectType[Backend, Target]()
}

trait GQLDrug {
  implicit val drugHasId = HasId[Drug, String](_.id)

  val drugsFetcherCache = FetcherCache.simple
  val drugsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(100).caching(drugsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDrugs(ids)
    })

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit val linkedDiseasesImp = deriveObjectType[Backend, LinkedDiseases]()
  implicit val linkedTargetsImp = deriveObjectType[Backend, LinkedTargets]()
  implicit val drugReferenceImp = deriveObjectType[Backend, DrugReference]()
  implicit val mechanismOfActionRowImp = deriveObjectType[Backend, MechanismOfActionRow]()
  implicit val mechanismOfActionImp = deriveObjectType[Backend, MechanismsOfAction]()
  implicit val withdrawnNoticeImp = deriveObjectType[Backend, WithdrawnNotice]()
  implicit val drugImp = deriveObjectType[Backend, Drug]()
}

object GQLSchema extends GQLMeta with GQLTarget with GQLDrug with GQLSearchResult {
  val resolvers = DeferredResolver.fetchers(targetsFetcher, drugsFetcher)

  implicit val paginationFormatImp = Json.format[Entities.Pagination]
  val pagination = deriveInputObjectType[Entities.Pagination]()
  val pageArg = Argument("page", OptionInputType(pagination))
  val queryString = Argument("queryString", StringType, description = "Query string")
  val ensemblId = Argument("ensemblId", StringType, description = "Ensembl ID" )
  val ensemblIds = Argument("ensemblIds", ListInputType(StringType), description = "List of Ensembl IDs")
  val chemblId = Argument("chemblId", StringType, description = "Chembl ID" )
  val chemblIds = Argument("chemblIds", ListInputType(StringType), description = "List of Chembl IDs")

  val searchUnionType = UnionType("SearchHitsResult", types = targetImp :: drugImp :: Nil)

  val searchHits = ObjectType("SearchHits",
    "Search results",
    fields[Backend, SearchResults](
      Field("totalHits", LongType,
        Some("Total results"),
        resolve = _.value.total),
      Field("hits", ListType(searchUnionType),
        Some("List of results"),
        resolve = ctx => {
          ctx.value.results.filter(_.entity != "disease").map(hit => hit.entity match {
            case "target" => targetsFetcher.defer(hit.id)
            case _ => drugsFetcher.defer(hit.id)
          })
        })
    ))

  val query = ObjectType(
    "Query", fields[Backend, Unit](
      Field("meta", metaImp,
        description = Some("Return Open Targets API metadata information"),
        arguments = Nil,
        resolve = ctx => ctx.ctx.getMeta),
      Field("target", OptionType(targetImp),
        description = Some("Return a Target"),
        arguments = ensemblId :: Nil,
        resolve = ctx => targetsFetcher.deferOpt(ctx.arg(ensemblId))),
      Field("targets", ListType(targetImp),
        description = Some("Return Targets"),
        arguments = ensemblIds :: Nil,
        resolve = ctx => targetsFetcher.deferSeqOpt(ctx.arg(ensemblIds))),
      Field("drug", OptionType(drugImp),
        description = Some("Return a drug"),
        arguments = chemblId :: Nil,
        resolve = ctx => drugsFetcher.deferOpt(ctx.arg(chemblId))),
      Field("drugs", ListType(drugImp),
        description = Some("Return drugs"),
        arguments = chemblIds :: Nil,
        resolve = ctx => drugsFetcher.deferSeqOpt(ctx.arg(chemblIds))),
      Field("searchUnion", searchHits,
        description = Some("Search"),
        arguments = queryString :: Nil,
        resolve = ctx => ctx.ctx.search(Seq("search_target", "search_disease", "search_drug"),
          ctx.arg(queryString), Some(0), Some(10))),
      Field("search", searchResultsImp,
        description = Some("Search"),
        arguments = queryString :: Nil,
        resolve = ctx => ctx.ctx.search(Seq("search_target", "search_disease", "search_drug"),
          ctx.arg(queryString), Some(0), Some(100)))

    ))

  val schema = Schema(query)
}
