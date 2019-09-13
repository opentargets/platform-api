package models

import play.api.libs.json.Json
import sangria.macros.derive._
import sangria.marshalling.playJson._
import sangria.schema._
import entities._
import Entities.JSONImplicits._
import sangria.execution.deferred.{DeferredResolver, Fetcher, FetcherConfig, HasId}

trait GQLMeta {
  implicit val metaVersionImp = deriveObjectType[Backend, Entities.MetaVersion]()
  implicit val metaImp = deriveObjectType[Backend, Entities.Meta]()
}

trait GQLTarget {
  implicit val targetHasId = HasId[Target, String](_.id)

  val targetsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(100),
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

  val drugsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(100),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDrugs(ids)
    })

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit val drugReferenceImp = deriveObjectType[Backend, DrugReference]()
  implicit val mechanismOfActionRowImp = deriveObjectType[Backend, MechanismOfActionRow]()
  implicit val mechanismOfActionImp = deriveObjectType[Backend, MechanismsOfAction]()
  implicit val withdrawnNoticeImp = deriveObjectType[Backend, WithdrawnNotice]()
  implicit val drugImp = deriveObjectType[Backend, Drug]()
}

object GQLSchema extends GQLMeta with GQLTarget with GQLDrug {
  val resolvers = DeferredResolver.fetchers(targetsFetcher, drugsFetcher)

  implicit val paginationFormatImp = Json.format[Entities.Pagination]
  val pagination = deriveInputObjectType[Entities.Pagination]()
  val pageArg = Argument("page", OptionInputType(pagination))
  val ensemblId = Argument("ensemblId", StringType, description = "Ensembl ID" )
  val ensemblIds = Argument("ensemblIds", ListInputType(StringType), description = "List of Ensembl IDs")
  val chemblId = Argument("chemblId", StringType, description = "Chembl ID" )
  val chemblIds = Argument("chemblIds", ListInputType(StringType), description = "List of Chembl IDs")

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
        description = Some("Return a Target"),
        arguments = ensemblIds :: Nil,
        resolve = ctx => targetsFetcher.deferSeqOpt(ctx.arg(ensemblIds))),
      Field("drug", OptionType(drugImp),
        description = Some("Return a Target"),
        arguments = chemblId :: Nil,
        resolve = ctx => drugsFetcher.deferOpt(ctx.arg(chemblId))),
      Field("drugs", ListType(drugImp),
        description = Some("Return a Target"),
        arguments = chemblIds :: Nil,
        resolve = ctx => drugsFetcher.deferSeqOpt(ctx.arg(chemblIds)))
    ))

  val schema = Schema(query)
}
