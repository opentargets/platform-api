package models

import play.api.libs.json.Json
import play.api.Logging
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
import play.api.Configuration
import play.api.mvc.CookieBaker
import sangria.execution.deferred._
import entities.MousePhenotypes
import entities.MousePhenotypes._
import entities.SearchResults._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.sort._
import models.entities.Interaction._
import models.gql.Objects._
import models.gql.Arguments._
import models.gql.Fetchers._

trait GQLEntities extends Logging {
}


object GQLSchema {
  val resolvers = DeferredResolver.fetchers(targetsFetcher,
    drugsFetcher,
    diseasesFetcher,
    ecosFetcher,
    reactomeFetcher,
    expressionFetcher,
    mousePhenotypeFetcher,
    otarProjectsFetcher,
    soTermsFetcher,
    indicationFetcher
  )


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
      Field("associationDatasources", ListType(evidenceSourceImp),
        description = Some("The complete list of all possible datasources"),
        resolve = ctx => ctx.ctx.getAssociationDatasources),
      Field("interactionResources", ListType(interactionResources),
        description = Some("The complete list of all possible datasources"),
        resolve = ctx => {
          import ctx.ctx._
          Interactions.listResources
        })
    ))

  val schema = Schema(query)
}
