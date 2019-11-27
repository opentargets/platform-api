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
import entities.Configuration._
import entities.Configuration.JSONImplicits._
import sangria.execution.deferred._

trait GQLArguments {
  implicit val paginationFormatImp = Json.format[Pagination]
  val pagination = deriveInputObjectType[Pagination]()
  val pageArg = Argument("page", OptionInputType(pagination))
  val queryString = Argument("queryString", StringType, description = "Query string")
  val ensemblId = Argument("ensemblId", StringType, description = "Ensembl ID" )
  val efoId = Argument("efoId", StringType, description = "EFO ID" )
  val networkExpansionId = Argument("networkExpansionId", OptionInputType(StringType), description = "Network expansion ID")
  val ensemblIds = Argument("ensemblIds", ListInputType(StringType), description = "List of Ensembl IDs")
  val chemblId = Argument("chemblId", StringType, description = "Chembl ID" )
  val chemblIds = Argument("chemblIds", ListInputType(StringType), description = "List of Chembl IDs")
}

trait GQLMeta {
  implicit val metaDataVersionImp = deriveObjectType[Backend, DataVersion]()
  implicit val metaAPIVersionImp = deriveObjectType[Backend, APIVersion]()
  implicit val metaImp = deriveObjectType[Backend, Meta]()
}

trait GQLEntities extends GQLArguments {

//  implicit val otEntity = InterfaceType("Entity", fields[Backend, OTEntity]())

  // target
  implicit val targetHasId = HasId[Target, String](_.id)

  val targetsFetcherCache = FetcherCache.simple
  val targetsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(Configuration.batchSize).caching(targetsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getTargets(ids)
    })

  implicit val datasourceSettingsJsonImp = Json.format[DatasourceSettings]
  val datasourceSettingsInputImp = deriveInputObjectType[DatasourceSettings](
    InputObjectTypeName("DatasourceSettingsInput")
  )
  val datasourceSettingsListArg = Argument("datasources",
     OptionInputType(ListInputType(datasourceSettingsInputImp)))

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit val proteinImp = deriveObjectType[Backend, Protein]()
  implicit val genomicLocationImp = deriveObjectType[Backend, GenomicLocation]()
  implicit lazy val targetImp: ObjectType[Backend, Target] = deriveObjectType(
    AddFields(
      Field("associationsOnTheFly", associationsImp,
        description = Some("Associations for a fixed target"),
        arguments = datasourceSettingsListArg :: networkExpansionId :: pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getAssociationsTargetFixed(ctx.value.id,
            ctx.arg(datasourceSettingsListArg),
            ctx.arg(networkExpansionId),
            ctx.arg(pageArg)))
  ))

  // drug
  implicit val drugHasId = HasId[Drug, String](_.id)

  val drugsFetcherCache = FetcherCache.simple
  val drugsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(Configuration.batchSize).caching(drugsFetcherCache),
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
  implicit lazy val linkedDiseasesImp = deriveObjectType[Backend, LinkedDiseases]()
  implicit lazy val linkedTargetsImp = deriveObjectType[Backend, LinkedTargets](
    ReplaceField("rows", Field("rows", ListType(targetImp), Some("Target List"),
      resolve = r => targetsFetcher.deferSeqOpt(r.value.rows)))
  )

  implicit lazy val drugReferenceImp = deriveObjectType[Backend, DrugReference]()
  implicit lazy val mechanismOfActionRowImp = deriveObjectType[Backend, MechanismOfActionRow](
    ReplaceField("targets", Field("targets", ListType(targetImp), Some("Target List"),
      resolve = r => targetsFetcher.deferSeqOpt(r.value.targets)))
  )

  implicit lazy val mechanismOfActionImp = deriveObjectType[Backend, MechanismsOfAction]()
  implicit lazy val withdrawnNoticeImp = deriveObjectType[Backend, WithdrawnNotice]()
  implicit lazy val drugImp = deriveObjectType[Backend, Drug]()

  implicit val datasourceSettingsImp = deriveObjectType[Backend, DatasourceSettings]()
  implicit val networkSettingsImp = deriveObjectType[Backend, LUTableSettings]()
  implicit val associationSettingsImp = deriveObjectType[Backend, AssociationSettings]()
  implicit val targetSettingsImp = deriveObjectType[Backend, TargetSettings]()
  implicit val diseaseSettingsImp = deriveObjectType[Backend, DiseaseSettings]()
  implicit val harmonicSettingsImp = deriveObjectType[Backend, HarmonicSettings]()
  implicit val clickhouseSettingsImp = deriveObjectType[Backend, ClickhouseSettings]()

  implicit lazy val networkNodeImp = deriveObjectType[Backend, NetworkNode]()
  implicit lazy val associationImp = deriveObjectType[Backend, Association]()
  implicit lazy val associationsImp = deriveObjectType[Backend, Associations]()

  // implement associations
  val associationsObTheFlyGQLImp = ObjectType("AssociationsOnTheFly",
    "Compute Associations on the fly",
    fields[Backend, Unit](
      Field("meta", clickhouseSettingsImp,
        Some("Meta information"),
        resolve = _.ctx.defaultOTSettings.clickhouse),
      Field("byTargetFixed", associationsImp,
        description = Some("Associations for a fixed target"),
        arguments = ensemblId :: datasourceSettingsListArg :: networkExpansionId :: pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getAssociationsTargetFixed(ctx.arg(ensemblId),
            ctx.arg(datasourceSettingsListArg),
            ctx.arg(networkExpansionId),
            ctx.arg(pageArg))),
      Field("byDiseaseFixed", associationsImp,
        description = Some("Associations for a fixed disease"),
        arguments = efoId :: datasourceSettingsListArg :: networkExpansionId :: pageArg :: Nil,
        resolve = ctx => ctx.ctx.getAssociationsDiseaseFixed(ctx.arg(efoId),
          ctx.arg(datasourceSettingsListArg),
          ctx.arg(networkExpansionId),
          ctx.arg(pageArg)))
    ))
}

object GQLSchema extends GQLMeta with GQLEntities {
  val resolvers = DeferredResolver.fetchers(targetsFetcher, drugsFetcher)

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
      Field("topHit", OptionType(msearchResultType),
        Some("Top Hit"),
        resolve = ctx => {
          ctx.value.topHit match {
            case Some(el) =>
              if (el.entity == "target") targetsFetcher.deferOpt(el.id)
              else drugsFetcher.deferOpt(el.id)
            case None => None
          }
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
      Field("search", searchResultsGQLImp,
        description = Some("Multi entity search"),
        arguments = queryString :: pageArg :: Nil,
        resolve = ctx => ctx.ctx.search(ctx.arg(queryString), ctx.arg(pageArg))),
      Field("altSearch", altSearchResultsImp,
        description = Some("Search"),
        arguments = queryString :: pageArg :: Nil,
        resolve = ctx => ctx.ctx.altSearch(ctx.arg(queryString), ctx.arg(pageArg))),
      Field("associationsOnTheFly", associationsObTheFlyGQLImp,
        Some("associations on the fly"),
        resolve = ctx => ctx.value)
    ))

  val schema = Schema(query)
}
