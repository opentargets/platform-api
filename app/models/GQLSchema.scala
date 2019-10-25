package models

import play.api.libs.json.Json
import sangria.schema._
import sangria.macros._
import sangria.macros.derive._
import sangria.ast
import sangria.execution._
import sangria.marshalling.playJson._
import sangria.schema.AstSchemaBuilder._
import sangria.util._
import entities._
import entities.Configuration.JSONImplicits._
import models.entities.Configuration.{Meta, MetaVersion}
import sangria.execution.deferred._

trait GQLMeta {
  implicit val metaVersionImp = deriveObjectType[Backend, MetaVersion]()
  implicit val metaImp = deriveObjectType[Backend, Meta]()
}

trait GQLEntities {

//  implicit val otEntity = InterfaceType("Entity", fields[Backend, OTEntity]())

  // target
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

  // drug
  implicit val drugHasId = HasId[Drug, String](_.id)

  val drugsFetcherCache = FetcherCache.simple
  val drugsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(100).caching(drugsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDrugs(ids)
    })

  implicit val searchResultAggsCategoryImp = deriveObjectType[Backend, models.entities.SearchResultAggCategory]()
  implicit val searchResultAggsEntityImp = deriveObjectType[Backend, models.entities.SearchResultAggEntity]()
  implicit val searchResultAggsImp = deriveObjectType[Backend, models.entities.SearchResultAggs]()
  implicit val searchResultImp = deriveObjectType[Backend, models.entities.SearchResult]()
  implicit val altSearchResultsImp = deriveObjectType[Backend, models.entities.AltSearchResults]()
  implicit val searchResultsImp = deriveObjectType[Backend, models.entities.SearchResults]()

  lazy val msearchResultType = UnionType("MSearchResultType", types = List(targetImp, drugImp))

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit val linkedDiseasesImp = deriveObjectType[Backend, LinkedDiseases]()
  implicit val linkedTargetsImp = deriveObjectType[Backend, LinkedTargets](
    ReplaceField("rows", Field("rows", ListType(targetImp), Some("Target List"),
      resolve = r => targetsFetcher.deferSeq(r.value.rows)))
  )

  implicit val drugReferenceImp = deriveObjectType[Backend, DrugReference]()
  implicit val mechanismOfActionRowImp = deriveObjectType[Backend, MechanismOfActionRow](
    ReplaceField("targets", Field("targets", ListType(targetImp), Some("Target List"),
      resolve = r => targetsFetcher.deferSeq(r.value.targets)))
  )

  implicit val mechanismOfActionImp = deriveObjectType[Backend, MechanismsOfAction]()
  implicit val withdrawnNoticeImp = deriveObjectType[Backend, WithdrawnNotice]()
  implicit val drugImp = deriveObjectType[Backend, Drug]()
}

object GQLSchema extends GQLMeta with GQLEntities {
  val resolvers = DeferredResolver.fetchers(targetsFetcher, drugsFetcher)

  implicit val paginationFormatImp = Json.format[Entities.Pagination]
  val pagination = deriveInputObjectType[Entities.Pagination]()
  val pageArg = Argument("page", OptionInputType(pagination))
  val queryString = Argument("queryString", StringType, description = "Query string")
  val ensemblId = Argument("ensemblId", StringType, description = "Ensembl ID" )
  val ensemblIds = Argument("ensemblIds", ListInputType(StringType), description = "List of Ensembl IDs")
  val chemblId = Argument("chemblId", StringType, description = "Chembl ID" )
  val chemblIds = Argument("chemblIds", ListInputType(StringType), description = "List of Chembl IDs")

  val searchResultsGQLImp = ObjectType("SearchResults",
    "Search results",
    fields[Backend, SearchResults](
      Field("totalHits", LongType,
        Some("Total results"),
        resolve = _.value.total),
      Field("targets", ListType(targetImp),
        description = Some("Return Targets"),
        resolve = ctx => targetsFetcher.deferSeqOpt(ctx.value.targets.map(_.id))),
      Field("aggregations", OptionType(searchResultAggsImp),
        description = Some("Aggregations"),
        resolve = ctx => ctx.value.aggregations),
      Field("drugs", ListType(drugImp),
        description = Some("Return drugs"),
        resolve = ctx => drugsFetcher.deferSeqOpt(ctx.value.drugs.map(_.id))),
      Field("topHit", OptionType(searchResultImp),
        Some("Top Hit"),
        resolve = ctx => ctx.value.topHit)

        //      Field("topHit", OptionType(msearchResultType),
//        Some("Top Hit"),
//        resolve = ctx => {
//          val th = ctx.value.topHit.map(h => h.entity match {
//            case "target" => targetsFetcher.deferOpt(h.id)
//            case "_" => drugsFetcher.deferOpt(h.id)
//          })

//          th
//        }
//      )
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
      Field("search", searchResultsGQLImp,
        description = Some("Multi entity search"),
        arguments = queryString :: pageArg :: Nil,
        resolve = ctx => ctx.ctx.search(ctx.arg(queryString), ctx.arg(pageArg))),
      Field("altSearch", altSearchResultsImp,
        description = Some("Search"),
        arguments = queryString :: pageArg :: Nil,
        resolve = ctx => ctx.ctx.altSearch(ctx.arg(queryString), ctx.arg(pageArg)))
    ))

  val schema = Schema(query)
}
