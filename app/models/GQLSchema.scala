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
    config = FetcherConfig.maxBatchSize(256),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getTargets(ids)
    })

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  val targetImp = deriveObjectType[Backend, Target](
    RenameField("approvedName", "name"),
    RenameField("approvedSymbol", "symbol"))
}

object GQLSchema extends GQLMeta with GQLTarget {
  val resolvers = DeferredResolver.fetchers(targetsFetcher)

  implicit val paginationFormatImp = Json.format[Entities.Pagination]
  val pagination = deriveInputObjectType[Entities.Pagination]()
  val pageArg = Argument("page", OptionInputType(pagination))
  val ensemblId = Argument("ennsemblId", StringType, description = "Ensembl ID" )
  val ensemblIds = Argument("ensemblIds", ListInputType(StringType), description = "List of Ensembl IDs")

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
        resolve = ctx => targetsFetcher.deferSeq(ctx.arg(ensemblIds)))
    ))

  val schema = Schema(query)
}
