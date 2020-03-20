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
  val entityNames = Argument("entityNames", OptionInputType(ListInputType(StringType)),
    description = "List of entity names to search for (target, disease, drug,...)")
  val pageArg = Argument("page", OptionInputType(pagination))
  val queryString = Argument("queryString", StringType, description = "Query string")
  val efoId = Argument("efoId", StringType, description = "EFO ID" )
  val efoIds = Argument("efoIds", ListInputType(StringType), description = "EFO ID" )
  val networkExpansionId = Argument("networkExpansionId", OptionInputType(StringType), description = "Network expansion ID")
  val ensemblId = Argument("ensemblId", StringType, description = "Ensembl ID" )
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
  val entityImp = InterfaceType("Entity", fields[Backend, Entity](
    Field("id", StringType, resolve = _.value.id)))

  // target
  implicit val targetHasId = HasId[Target, String](_.id)

  val targetsFetcherCache = FetcherCache.simple
  val targetsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(Configuration.batchSize).caching(targetsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getTargets(ids)
    })

  // disease
  implicit val diseaseHasId = HasId[Disease, String](_.id)

  val diseasesFetcherCache = FetcherCache.simple
  val diseasesFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(Configuration.batchSize).caching(diseasesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDiseases(ids)
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
      Field("cancerBiomarkers", OptionType(cancerBiomarkersImp),
        description = Some("CancerBiomarkers"),
        arguments = pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getCancerBiomarkers(
            Map("target" -> ctx.value.id),
            ctx.arg(pageArg))),

      Field("associationsOnTheFly", associationsImp,
        description = Some("Associations for a fixed target"),
        arguments = datasourceSettingsListArg :: networkExpansionId :: pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getAssociationsTargetFixed(ctx.value.id,
            ctx.arg(datasourceSettingsListArg),
            ctx.arg(networkExpansionId),
            ctx.arg(pageArg)))
  ))

  // disease
  implicit lazy val diseaseImp: ObjectType[Backend, Disease] = deriveObjectType(
    ReplaceField("therapeuticAreas", Field("therapeuticAreas",
      ListType(diseaseImp), Some("Disease List"),
      resolve = r => diseasesFetcher.deferSeq(r.value.therapeuticAreas))),
    AddFields(
      Field("associationsOnTheFly", associationsImp,
        description = Some("Associations for a fixed disease"),
        arguments = datasourceSettingsListArg :: networkExpansionId :: pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getAssociationsDiseaseFixed(ctx.value.id,
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


  // cancerbiomarkers
  implicit val cancerBiomarkerSourceImp = deriveObjectType[Backend, CancerBiomarkerSource]()
  implicit val cancerBiomarkerImp = deriveObjectType[Backend, CancerBiomarker](
    ReplaceField("target", Field("target", targetImp, Some("Target"),
      resolve = r => targetsFetcher.defer(r.value.target))),
    ReplaceField("disease", Field("disease", diseaseImp, Some("Disease"),
      resolve = r => diseasesFetcher.defer(r.value.disease)))
  )

  implicit val cancerBiomarkersImp = deriveObjectType[Backend, CancerBiomarkers]()

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit lazy val linkedDiseasesImp = deriveObjectType[Backend, LinkedDiseases]()
  implicit lazy val linkedTargetsImp = deriveObjectType[Backend, LinkedTargets](
    ReplaceField("rows", Field("rows", ListType(targetImp), Some("Target List"),
      resolve = r => targetsFetcher.deferSeqOpt(r.value.rows)))
  )

  implicit lazy val drugReferenceImp = deriveObjectType[Backend, Reference]()
  implicit lazy val mechanismOfActionRowImp = deriveObjectType[Backend, MechanismOfActionRow](
    ReplaceField("targets", Field("targets", ListType(targetImp), Some("Target List"),
      resolve = r => targetsFetcher.deferSeqOpt(r.value.targets)))
  )

  implicit lazy val indicationRowImp = deriveObjectType[Backend, IndicationRow](
    ReplaceField("disease", Field("disease", diseaseImp, Some("Disease"),
      resolve = r => diseasesFetcher.defer(r.value.disease)))
  )

  implicit lazy val indicationsImp = deriveObjectType[Backend, Indications]()

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
  val resolvers = DeferredResolver.fetchers(targetsFetcher, drugsFetcher, diseasesFetcher)


  lazy val msearchResultType = UnionType("EntityUnionType", types = List(targetImp, drugImp, diseaseImp))

  implicit val searchResultAggsCategoryImp = deriveObjectType[Backend, models.entities.SearchResultAggCategory]()
  implicit val searchResultAggsEntityImp = deriveObjectType[Backend, models.entities.SearchResultAggEntity]()
  implicit val searchResultAggsImp = deriveObjectType[Backend, models.entities.SearchResultAggs]()
  implicit val searchResultImp = deriveObjectType[Backend, models.entities.SearchResult](
    AddFields(
      Field("object", OptionType(msearchResultType),
        description = Some("Associations for a fixed target"),
        resolve = ctx => {
          ctx.value.entity match {
            case "target" => targetsFetcher.deferOpt(ctx.value.id)
            case "disease" => diseasesFetcher.deferOpt(ctx.value.id)
            case _ => drugsFetcher.deferOpt(ctx.value.id)
          }
        }))
  )
  implicit val searchResultsImp = deriveObjectType[Backend, models.entities.SearchResults]()

  val searchResultsGQLImp = ObjectType("SearchResults",
    "Search results",
    fields[Backend, SearchResults](
      Field("aggregations", OptionType(searchResultAggsImp),
        description = Some("Aggregations"),
        resolve = _.value.aggregations),
      Field("hits", ListType(searchResultImp),
        description = Some("Return combined"),
        resolve = _.value.hits),
      Field("total", LongType,
        description = Some("Total number or results given a entity filter"),
        resolve = _.value.total)
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
      Field("disease", OptionType(diseaseImp),
        description = Some("Return a Disease"),
        arguments = efoId :: Nil,
        resolve = ctx => diseasesFetcher.deferOpt(ctx.arg(efoId))),
      Field("diseases", ListType(diseaseImp),
        description = Some("Return Diseases"),
        arguments = efoIds :: Nil,
        resolve = ctx => diseasesFetcher.deferSeqOpt(ctx.arg(efoIds))),
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
        arguments = queryString :: entityNames :: pageArg :: Nil,
        resolve = ctx => {
          val entities = ctx.arg(entityNames).getOrElse(Seq.empty)
          ctx.ctx.search(ctx.arg(queryString), ctx.arg(pageArg), entities)
        }),
      Field("associationsOnTheFly", associationsObTheFlyGQLImp,
        Some("associations on the fly"),
        resolve = ctx => ctx.value)
    ))

  val schema = Schema(query)
}
