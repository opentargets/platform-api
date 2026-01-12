package models

import sangria.schema.*
import entities.*
import models.entities.CredibleSets.credibleSetsImp
import models.entities.Studies.studiesImp
import sangria.execution.deferred.*
import gql.validators.QueryTermsValidator.*

import scala.concurrent.ExecutionContext.Implicits.global
import models.gql.Objects.*
import models.gql.Arguments.*
import models.gql.Fetchers.*
import models.gql.DeferredResolvers.*

import scala.concurrent.*
import scala.util.{Failure, Success}
import models.Helpers.ComplexityCalculator.*
import utils.OTLogging

trait GQLEntities extends OTLogging {}

object GQLSchema {
  val resolvers: DeferredResolver[Backend] = deferredResolvers

  val query: ObjectType[Backend, Unit] = ObjectType(
    "Query",
    "Root query type providing access to all entities and search functionality in the Open Targets Platform. Supports retrieval of targets, diseases, drugs, variants, studies, credible sets, and their associations. Includes full-text search, mapping, and filtering capabilities.",
    fields[Backend, Unit](
      Field(
        "meta",
        metaImp,
        description =
          Some("Open Targets API metadata, including version and configuration information"),
        arguments = Nil,
        resolve = ctx => ctx.ctx.getMeta
      ),
      Field(
        "target",
        OptionType(targetImp),
        description =
          Some("Retrieve a target (gene/protein) by target identifier (e.g. ENSG00000139618)"),
        arguments = ensemblId :: Nil,
        resolve = ctx => targetsFetcher.deferOpt(ctx.arg(ensemblId))
      ),
      Field(
        "targets",
        ListType(targetImp),
        description = Some("Retrieve multiple targets by target identifiers"),
        arguments = ensemblIds :: Nil,
        complexity = Some(complexityCalculator(ensemblIds)),
        resolve = ctx => targetsFetcher.deferSeqOpt(ctx.arg(ensemblIds))
      ),
      Field(
        "disease",
        OptionType(diseaseImp),
        description = Some("Retrieve a disease or phenotype by identifier (e.g. EFO_0000400)"),
        arguments = efoId :: Nil,
        resolve = ctx => diseasesFetcher.deferOpt(ctx.arg(efoId))
      ),
      Field(
        "diseases",
        ListType(diseaseImp),
        description = Some("Retrieve multiple diseases by disease or phenotype identifiers"),
        arguments = efoIds :: Nil,
        complexity = Some(complexityCalculator(efoIds)),
        resolve = ctx => diseasesFetcher.deferSeqOpt(ctx.arg(efoIds))
      ),
      Field(
        "drug",
        OptionType(drugImp),
        description = Some("Retrieve a drug or clinical candidate by identifier (e.g. CHEMBL112)"),
        arguments = chemblId :: Nil,
        resolve = ctx => drugsFetcher.deferOpt(ctx.arg(chemblId))
      ),
      Field(
        "drugs",
        ListType(drugImp),
        description = Some("Retrieve multiple drugs or clinical candidates by identifiers"),
        arguments = chemblIds :: Nil,
        complexity = Some(complexityCalculator(chemblIds)),
        resolve = ctx => drugsFetcher.deferSeqOpt(ctx.arg(chemblIds))
      ),
      Field(
        "search",
        searchResultsGQLImp,
        description = Some(
          "Full-text, multi-entity search across all types of entities (targets, diseases, drugs, variants or studies)"
        ),
        arguments = queryString :: entityNames :: pageArg :: Nil,
        resolve = ctx => {
          val entities = ctx.arg(entityNames).getOrElse(Seq.empty)
          ctx.ctx.search(ctx.arg(queryString), ctx.arg(pageArg), entities)
        }
      ),
      Field(
        "facets",
        searchFacetsResultsGQLImp,
        description = Some("Search sets of targets or diseases used to facet associations"),
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
        description = Some(
          "Map free-text terms to canonical IDs used as primary identifiers in the Platform (targets, diseases, drugs, variants or studies). For example, mapping 'diabetes' to EFO_0000400 or 'BRCA1' to ENSG00000139618"
        ),
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
        description = Some("List of available evidence datasources and their datatypes"),
        resolve = ctx => ctx.ctx.getAssociationDatasources
      ),
      Field(
        "interactionResources",
        ListType(interactionResources),
        description = Some("List of molecular interaction resources and their versions"),
        resolve = ctx => {
          import ctx.ctx._
          Interactions.listResources
        }
      ),
      Field(
        "geneOntologyTerms",
        ListType(OptionType(geneOntologyTermImp)),
        description = Some("Fetch Gene Ontology terms by GO identifiers"),
        arguments = goIds :: Nil,
        complexity = Some(complexityCalculator(goIds)),
        resolve = ctx => goFetcher.deferSeqOptExplicit(ctx.arg(goIds))
      ),
      Field(
        "variant",
        OptionType(variantIndexImp),
        description = Some(
          "Retrieve a variant by identifier in the format of CHROM_POS_REF_ALT for SNPs and short indels (e.g. 19_44908684_T_C)"
        ),
        arguments = variantId :: Nil,
        resolve = ctx => variantFetcher.deferOpt(ctx.arg(variantId))
      ),
      Field(
        "study",
        OptionType(studyImp),
        description = Some("Retrieve a GWAS or molecular QTL study by ID (e.g. GCST004131)"),
        arguments = studyId :: Nil,
        resolve = ctx => studyFetcher.deferOpt(ctx.arg(studyId))
      ),
      Field(
        "studies",
        studiesImp,
        description = Some(
          "List GWAS or molecular QTL studies filtered by ID(s) and/or disease(s); supports ontology expansion"
        ),
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
        description = Some("Retrieve a 95% credible set (study-locus) by identifier"),
        arguments = studyLocusId :: Nil,
        resolve = ctx => credibleSetFetcher.deferOpt(ctx.arg(studyLocusId))
      ),
      Field(
        "credibleSets",
        credibleSetsImp,
        description = Some(
          "List credible sets filtered by study-locus IDs, study IDs, variant IDs, study types or regions"
        ),
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
