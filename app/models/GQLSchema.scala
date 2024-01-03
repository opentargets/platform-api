package models

import play.api.Logging
import sangria.schema._
import entities._
import sangria.execution.deferred._

import scala.concurrent.ExecutionContext.Implicits.global
import models.entities.Interaction._
import models.gql.Objects._
import models.gql.Arguments._
import models.gql.Fetchers._

trait GQLEntities extends Logging {}

object GQLSchema {
  val resolvers: DeferredResolver[Backend] = DeferredResolver.fetchers(
    targetsFetcher,
    drugsFetcher,
    diseasesFetcher,
    hposFetcher,
    reactomeFetcher,
    expressionFetcher,
    otarProjectsFetcher,
    soTermsFetcher,
    indicationFetcher,
    goFetcher
  )

  val query: ObjectType[Backend, Unit] = ObjectType(
    "Query",
    fields[Backend, Unit](
      Field(
        "meta",
        metaImp,
        description = Some("Return Open Targets API metadata information"),
        arguments = Nil,
        resolve = ctx => ctx.ctx.getMeta
      ),
      Field(
        "target",
        OptionType(targetImp),
        description = Some("Return a Target"),
        arguments = ensemblId :: Nil,
        resolve = ctx => targetsFetcher.deferOpt(ctx.arg(ensemblId))
      ),
      Field(
        "targets",
        ListType(targetImp),
        description = Some("Return Targets"),
        arguments = ensemblIds :: Nil,
        resolve = ctx => targetsFetcher.deferSeqOpt(ctx.arg(ensemblIds))
      ),
      Field(
        "disease",
        OptionType(diseaseImp),
        description = Some("Return a Disease"),
        arguments = efoId :: Nil,
        resolve = ctx => diseasesFetcher.deferOpt(ctx.arg(efoId))
      ),
      Field(
        "diseases",
        ListType(diseaseImp),
        description = Some("Return Diseases"),
        arguments = efoIds :: Nil,
        resolve = ctx => diseasesFetcher.deferSeqOpt(ctx.arg(efoIds))
      ),
      Field(
        "drug",
        OptionType(drugImp),
        description = Some("Return a drug"),
        arguments = chemblId :: Nil,
        resolve = ctx => drugsFetcher.deferOpt(ctx.arg(chemblId))
      ),
      Field(
        "drugs",
        ListType(drugImp),
        description = Some("Return drugs"),
        arguments = chemblIds :: Nil,
        resolve = ctx => drugsFetcher.deferSeqOpt(ctx.arg(chemblIds))
      ),
      Field(
        "search",
        searchResultsGQLImp,
        description = Some("Multi entity search"),
        arguments = queryString :: entityNames :: pageArg :: Nil,
        resolve = ctx => {
          val entities = ctx.arg(entityNames).getOrElse(Seq.empty)
          ctx.ctx.search(ctx.arg(queryString), ctx.arg(pageArg), entities)
        }
      ),
      Field(
        "mapIds",
        mappingResultsImp,
        description = Some("Map terms to IDs"),
        arguments = queryTerms :: entityNames :: Nil,
        resolve = ctx => {
          val entities = ctx.arg(entityNames).getOrElse(Seq.empty)
          ctx.ctx.mapIds(ctx.arg(queryTerms), entities)
        }
      ),
      Field(
        "associationDatasources",
        ListType(evidenceSourceImp),
        description = Some("The complete list of all possible datasources"),
        resolve = ctx => ctx.ctx.getAssociationDatasources
      ),
      Field(
        "interactionResources",
        ListType(interactionResources),
        description = Some("The complete list of all possible datasources"),
        resolve = ctx => {
          import ctx.ctx._
          Interactions.listResources
        }
      ),
      Field(
        "geneOntologyTerms",
        ListType(OptionType(geneOntologyTermImp)),
        description = Some("Gene ontology terms"),
        arguments = goIds :: Nil,
        resolve = ctx => goFetcher.deferSeqOptExplicit(ctx.arg(goIds))
      )
    )
  )

  val schema: Schema[Backend, Unit] = Schema(query)
}
