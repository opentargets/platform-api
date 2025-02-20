package models

import play.api.Logging
import play.api.libs.json._
import sangria.schema._
import entities._
import models.entities.CredibleSets.credibleSetsImp
import models.entities.Studies.studiesImp
import sangria.execution.deferred._
import gql.validators.QueryTermsValidator._
import scala.concurrent.ExecutionContext.Implicits.global
import models.entities.Interaction._
import models.gql.Objects._
import models.gql.Arguments._
import models.gql.Fetchers._
import models.gql.DeferredResolvers._
import scala.concurrent._
import scala.util.{Try, Failure, Success}
import models.Helpers.ComplexityCalculator._

trait GQLEntities extends Logging {}

object GQLSchema {
  val resolvers: DeferredResolver[Backend] = deferredResolvers

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
        complexity = Some(complexityCalculator(ensemblIds)),
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
        complexity = Some(complexityCalculator(efoIds)),
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
        complexity = Some(complexityCalculator(chemblIds)),
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
        "facets",
        searchFacetsResultsGQLImp,
        description = Some("Search facets"),
        arguments = optQueryString :: entityNames :: category :: pageArg :: Nil,
        resolve = ctx => {
          val queryString = ctx.arg(optQueryString).getOrElse("")
          val entities = ctx.arg(entityNames).getOrElse(Seq.empty)
          ctx.ctx.searchFacets(queryString, ctx.arg(pageArg), entities, ctx.arg(category))
        }
      ),
      Field(
        "mapIds",
        mappingResultsImp,
        description = Some("Map terms to IDs"),
        arguments = queryTerms :: entityNames :: Nil,
        resolve = ctx => {
          val entities = ctx.arg(entityNames).getOrElse(Seq.empty)
          withQueryTermsNumberValidation(ctx.arg(queryTerms), Pagination.sizeMax) match {
            case Success(terms) => ctx.ctx.mapIds(terms, entities)
            case Failure(error) => Future.failed(error)
          }
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
        complexity = Some(complexityCalculator(goIds)),
        resolve = ctx => goFetcher.deferSeqOptExplicit(ctx.arg(goIds))
      ),
      Field(
        "variant",
        OptionType(variantIndexImp),
        description = Some("Return a Variant"),
        arguments = variantId :: Nil,
        resolve = ctx => variantFetcher.deferOpt(ctx.arg(variantId))
      ),
      Field(
        "study",
        OptionType(studyImp),
        description = Some("Return a Study"),
        arguments = studyId :: Nil,
        resolve = ctx => studyFetcher.deferOpt(ctx.arg(studyId))
      ),
      Field(
        "studies",
        studiesImp,
        description = Some("Return a studies"),
        arguments = pageArg :: studyId :: diseaseIds :: enableIndirect :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = ctx => {
          val studyIdSeq =
            if (ctx.arg(studyId).isDefined) Seq(ctx.arg(studyId).get).filter(_ != "") else Seq.empty
          val diseaseIdsSeq = ctx.arg(diseaseIds).getOrElse(Seq.empty)
          val studyQueryArgs = StudyQueryArgs(
            studyIdSeq,
            diseaseIdsSeq,
            ctx.arg(enableIndirect).getOrElse(false)
          )
          ctx.ctx.getStudies(studyQueryArgs, ctx.arg(pageArg))
        }
      ),
      Field(
        "credibleSet",
        OptionType(credibleSetImp),
        description = Some("Return a Credible Set"),
        arguments = studyLocusId :: Nil,
        resolve = ctx => credibleSetFetcher.deferOpt(ctx.arg(studyLocusId))
      ),
      Field(
        "credibleSets",
        credibleSetsImp,
        description = None,
        arguments =
          pageArg :: studyLocusIds :: studyIds :: variantIds :: studyTypes :: regions :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = ctx => {
          val credSetQueryArgs = CredibleSetQueryArgs(
            ctx.arg(studyLocusIds).getOrElse(Seq.empty),
            ctx.arg(studyIds).getOrElse(Seq.empty),
            ctx.arg(variantIds).getOrElse(Seq.empty),
            ctx.arg(studyTypes).getOrElse(Seq.empty),
            ctx.arg(regions).getOrElse(Seq.empty)
          )
          ctx.ctx.getCredibleSets(credSetQueryArgs, ctx.arg(pageArg))
        }
      )
    )
  )

  val schema: Schema[Backend, Unit] = Schema(query)
}
