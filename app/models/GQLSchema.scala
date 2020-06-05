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
import play.api.{Configuration, Logger}
import play.api.mvc.CookieBaker
import sangria.execution.deferred._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.sort._

import models.entities.Configuration._

trait GQLArguments {
  implicit val paginationFormatImp = Json.format[Pagination]
  implicit val sortOrderImp = deriveEnumType[SortOrder]()
  implicit val sortEntityImp = deriveEnumType[SortEntity]()

  val sortFieldArg = Argument("sortField", OptionInputType(sortEntityImp), description = "Sort field name")
  val sortOrderArg = Argument("sortOrder", OptionInputType(sortOrderImp), description = "Sort type")
  val pagination = deriveInputObjectType[Pagination]()
  val entityNames = Argument("entityNames", OptionInputType(ListInputType(StringType)),
    description = "List of entity names to search for (target, disease, drug,...)")
  val pageArg = Argument("page", OptionInputType(pagination))
  val queryString = Argument("queryString", StringType, description = "Query string")
  val freeTextQuery = Argument("freeTextQuery", OptionInputType(StringType), description = "Query string")
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
  val logger = Logger(this.getClass)

  val entityImp = InterfaceType("Entity", fields[Backend, Entity](
    Field("id", StringType, resolve = _.value.id)))

  // target
  implicit val targetHasId = HasId[Target, String](_.id)

  val targetsFetcherCache = FetcherCache.simple
  val targetsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(targetsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getTargets(ids)
    })

  // disease
  implicit val diseaseHasId = HasId[Disease, String](_.id)

  val diseasesFetcherCache = FetcherCache.simple
  val diseasesFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(diseasesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDiseases(ids)
    })

  implicit val expressionHasId = HasId[Expressions, String](_.id)

  val expressionFetcherCache = FetcherCache.simple
  val expressionFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(expressionFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getExpressions(ids)
    })

  implicit val mousePhenotypeHasId = HasId[MousePhenotypes, String](_.id)

  val mousePhenotypeFetcherCache = FetcherCache.simple
  val mousePhenotypeFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(mousePhenotypeFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getMousePhenotypes(ids)
    })

  implicit val reactomeHasId = HasId[Reactome, String](_.id)

  val reactomeFetcherCache = FetcherCache.simple
  val reactomeFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(reactomeFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getReactomeNodes(ids)
    })

  implicit val ecoHasId = HasId[ECO, String](_.id)

  val ecosFetcherCache = FetcherCache.simple
  val ecosFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(diseasesFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getECOs(ids)
    })

  implicit val otherModalitiesCategoriesImp = deriveObjectType[Backend, OtherModalitiesCategories]()
  implicit val otherModalitiesImp = deriveObjectType[Backend, OtherModalities]()
  implicit val tractabilityAntibodyCategoriesImp = deriveObjectType[Backend, TractabilityAntibodyCategories]()
  implicit val tractabilitySmallMoleculeCategoriesImp = deriveObjectType[Backend, TractabilitySmallMoleculeCategories]()

  implicit val tractabilityAntibodyImp = deriveObjectType[Backend, TractabilityAntibody]()
  implicit val tractabilitySmallMoleculeImp = deriveObjectType[Backend, TractabilitySmallMolecule]()
  implicit val tractabilityImp = deriveObjectType[Backend, Tractability]()

  implicit val relatedTargetImp = deriveObjectType[Backend, DDRelation](
    ObjectTypeName("RelatedTarget"),
    ObjectTypeDescription("Related Target Entity"),
    ExcludeFields("A"),
    ReplaceField("B", Field("B",
      targetImp, Some("Target"),
      resolve = r => targetsFetcher.defer(r.value.B)))
  )

  implicit val relatedDiseaseImp = deriveObjectType[Backend, DDRelation](
    ObjectTypeName("RelatedDisease"),
    ObjectTypeDescription("Related Disease Entity"),
    ExcludeFields("A"),
    ReplaceField("B", Field("B",
      diseaseImp, Some("Disease"),
      resolve = r => diseasesFetcher.defer(r.value.B)))
  )

  implicit val relatedTargetsImp = deriveObjectType[Backend, DDRelations](
    ObjectTypeName("RelatedTargets"),
    ObjectTypeDescription("Related Targets Entity"),
    ReplaceField("rows", Field("rows",
      ListType(relatedTargetImp), Some("Related Targets"),
      resolve = r => r.value.rows))
  )

  implicit val relatedDiseasesImp = deriveObjectType[Backend, DDRelations](
    ObjectTypeName("RelatedDiseases"),
    ObjectTypeDescription("Related Diseases"),
    ReplaceField("rows", Field("rows",
      ListType(relatedDiseaseImp), Some("Related Diseases"),
      resolve = r => r.value.rows))
  )

  implicit val ecoImp = deriveObjectType[Backend, ECO]()

  lazy implicit val reactomeImp: ObjectType[Backend, Reactome] = deriveObjectType[Backend, Reactome](
    AddFields(
      Field("isRoot", BooleanType,
        description = Some("If the node is root"),
        resolve = _.value.isRoot)
    ),
    ReplaceField("children", Field("children",
      ListType(reactomeImp), Some("Reactome Nodes"),
      resolve = r => reactomeFetcher.deferSeqOpt(r.value.children))
    ),
    ReplaceField("parents", Field("parents",
      ListType(reactomeImp), Some("Reactome Nodes"),
      resolve = r => reactomeFetcher.deferSeqOpt(r.value.parents))
    ),
    ReplaceField("ancestors", Field("ancestors",
      ListType(reactomeImp), Some("Reactome Nodes"),
      resolve = r => reactomeFetcher.deferSeqOpt(r.value.ancestors))
    )
  )

  implicit val tissueImp = deriveObjectType[Backend, Tissue]()
  implicit val rnaExpressionImp = deriveObjectType[Backend, RNAExpression]()
  implicit val cellTypeImp = deriveObjectType[Backend, CellType]()
  implicit val proteinExpressionImp = deriveObjectType[Backend, ProteinExpression]()
  implicit val expressionImp = deriveObjectType[Backend, Expression]()
  implicit val expressionsImp = deriveObjectType[Backend, Expressions](
    ExcludeFields("id")
  )

  implicit val adverseEventImp = deriveObjectType[Backend, AdverseEvent]()
  implicit val adverseEventsImp = deriveObjectType[Backend, AdverseEvents]()

  implicit val datasourceSettingsJsonImp = Json.format[DatasourceSettings]
  val datasourceSettingsInputImp = deriveInputObjectType[DatasourceSettings](
    InputObjectTypeName("DatasourceSettingsInput")
  )
  val datasourceSettingsListArg = Argument("datasources",
     OptionInputType(ListInputType(datasourceSettingsInputImp)))

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit val geneObtologyImp = deriveObjectType[Backend, GeneOntology](
    ReplaceField("evidence", Field("evidence",
      ecoImp, Some("ECO object"),
      resolve = r => ecosFetcher.defer(r.value.evidence)))
  )

  implicit val literatureReferenceImp = deriveObjectType[Backend, LiteratureReference]()
  implicit val cancerHallmarkImp = deriveObjectType[Backend, CancerHallmark]()
  implicit val hallmarksAttributeImp = deriveObjectType[Backend, HallmarkAttribute]()
  implicit val hallmarksImp = deriveObjectType[Backend, Hallmarks]()

//  implicit val orthologImp = deriveObjectType[Backend, Ortholog]()
//  implicit val orthologsImp = deriveObjectType[Backend, Orthologs]()

  implicit val sourceLinkImp = deriveObjectType[Backend, SourceLink]()
  implicit val portalProbeImp = deriveObjectType[Backend, PortalProbe]()
  implicit val chemicalProbesImp = deriveObjectType[Backend, ChemicalProbes]()

  implicit val experimentDetailsImp = deriveObjectType[Backend, ExperimentDetails]()
  implicit val experimentalToxicityImp = deriveObjectType[Backend, ExperimentalToxicity]()
  implicit val safetyCodeImp = deriveObjectType[Backend, SafetyCode]()
  implicit val safetyReferenceImp = deriveObjectType[Backend, SafetyReference]()
  implicit val adverseEffectsActivationEffectsImp = deriveObjectType[Backend, AdverseEffectsActivationEffects]()
  implicit val adverseEffectsInhibitionEffectsImp = deriveObjectType[Backend, AdverseEffectsInhibitionEffects]()
  implicit val adverseEffectsImp = deriveObjectType[Backend, AdverseEffects]()
  implicit val safetyRiskInfoImp = deriveObjectType[Backend, SafetyRiskInfo]()
  implicit val safetyImp = deriveObjectType[Backend, Safety]()

  implicit val genotypePhenotypeImp = deriveObjectType[Backend, GenotypePhenotype]()
  implicit val mousePhenotypeImp = deriveObjectType[Backend, MousePhenotype]()
  implicit val mouseGeneImp = deriveObjectType[Backend, MouseGene]()
  implicit val mousePhenotypesImp = deriveObjectType[Backend, MousePhenotypes]()

  implicit val tepImp = deriveObjectType[Backend, Tep]()

  implicit val proteinClassPathNodeImp = deriveObjectType[Backend, ProteinClassPathNode]()
  implicit val proteinClassPathImp = deriveObjectType[Backend, ProteinClassPath]()

  implicit val proteinImp = deriveObjectType[Backend, ProteinAnnotations]()
  implicit val genomicLocationImp = deriveObjectType[Backend, GenomicLocation]()
  implicit lazy val targetImp: ObjectType[Backend, Target] = deriveObjectType(
    ReplaceField("reactome", Field("reactome",
      ListType(reactomeImp), Some("Reactome node list"),
      resolve = r => reactomeFetcher.deferSeq(r.value.reactome))),
    AddFields(
      Field("mousePhenotypes", ListType(mouseGeneImp),
        description = Some("Mouse phenotypes by linked mouse Gene"),
        resolve = r => DeferredValue(mousePhenotypeFetcher.deferOpt(r.value.id)).map {
          case Some(mouseGenes) => mouseGenes.rows
          case None => Seq.empty
        }),
      Field("expressions", ListType(expressionImp),
        description = Some("Protein and baseline expression for this target"),
        resolve = r => DeferredValue(expressionFetcher.deferOpt(r.value.id)).map {
          case Some(expressions) => expressions.rows
          case None => Seq.empty
        }),
      Field("knownDrugs", OptionType(knownDrugsImp),
        description = Some("Clinical Trial Drugs from evidences"),
        arguments = freeTextQuery :: pageArg :: Nil,
        resolve = ctx => {
          ctx.ctx.getKnownDrugs(ctx.arg(freeTextQuery).getOrElse(""),
            Map("target.raw" -> ctx.value.id),
            ctx.arg(pageArg)
          )
        }
      ),
      Field("cancerBiomarkers", OptionType(cancerBiomarkersImp),
        description = Some("CancerBiomarkers"),
        arguments = pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getCancerBiomarkers(
            Map("target.keyword" -> ctx.value.id),
            ctx.arg(pageArg))),

      Field("relatedTargets", OptionType(relatedTargetsImp),
        description = Some("Related Targets"),
        arguments = pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getRelatedTargets(
            Map("A.keyword" -> ctx.value.id),
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

  implicit val phenotypeImp = deriveObjectType[Backend, Phenotype]()
  // disease
  implicit lazy val diseaseImp: ObjectType[Backend, Disease] = deriveObjectType(
    ReplaceField("therapeuticAreas", Field("therapeuticAreas",
      ListType(diseaseImp), Some("Disease List"),
      resolve = r => diseasesFetcher.deferSeq(r.value.therapeuticAreas))),
//    ReplaceField("phenotypes", Field("phenotypes",
//      ListType(diseaseImp), Some("Phenotype List"),
//      resolve = r => diseasesFetcher.deferSeq(r.value.phenotypes))),
    ReplaceField("parents", Field("parents",
      ListType(diseaseImp), Some("Disease Parents List"),
      resolve = r => diseasesFetcher.deferSeq(r.value.parents))),
    ReplaceField("children", Field("children",
      ListType(diseaseImp), Some("Disease Children List"),
      resolve = r => diseasesFetcher.deferSeq(r.value.children))),
    // this query uses id and descendants fields to search for indirect diseases
    AddFields(
      Field("knownDrugs", OptionType(knownDrugsImp),
        description = Some("Clinical Trial Drugs from evidences"),
        arguments = freeTextQuery :: pageArg :: Nil,
        resolve = ctx => {
          ctx.ctx.getKnownDrugs(
            ctx.arg(freeTextQuery).getOrElse(""),
            Map(
              "disease.raw" -> ctx.value.id,
              "descendants.raw" -> ctx.value.id
            ),
            ctx.arg(pageArg))
        }
      ),

      Field("relatedDiseases", OptionType(relatedDiseasesImp),
        description = Some("Related Targets"),
        arguments = pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getRelatedDiseases(
            Map("A.keyword" -> ctx.value.id),
            ctx.arg(pageArg))),

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
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(drugsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getDrugs(ids)
    })

  // cancerbiomarkers
  implicit val cancerBiomarkerSourceImp = deriveObjectType[Backend, CancerBiomarkerSource]()
  implicit val cancerBiomarkerImp = deriveObjectType[Backend, CancerBiomarker](
    ReplaceField("target", Field("target", targetImp, Some("Target"),
      resolve = r => targetsFetcher.defer(r.value.target))),
    ReplaceField("disease", Field("disease", OptionType(diseaseImp), Some("Disease"),
      resolve = r => diseasesFetcher.deferOpt(r.value.disease)))
  )

  implicit val cancerBiomarkersImp = deriveObjectType[Backend, CancerBiomarkers]()

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit lazy val linkedDiseasesImp = deriveObjectType[Backend, LinkedIds](
    ObjectTypeName("LinkedDiseases"),
    ObjectTypeDescription("Linked Disease Entities"),
    ReplaceField("rows", Field("rows", ListType(diseaseImp), Some("Disease List"),
      resolve = r => diseasesFetcher.deferSeqOpt(r.value.rows)))
  )

  implicit lazy val linkedTargetsImp = deriveObjectType[Backend, LinkedIds](
    ObjectTypeName("LinkedTargets"),
    ObjectTypeDescription("Linked Target Entities"),
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
  implicit lazy val drugImp = deriveObjectType[Backend, Drug](
    AddFields(
      Field("knownDrugs", OptionType(knownDrugsImp),
        description = Some("Clinical Trial Drugs from evidences"),
        arguments = freeTextQuery :: pageArg :: Nil,
        resolve = ctx => {
          ctx.ctx.getKnownDrugs(
            ctx.arg(freeTextQuery).getOrElse(""),
            Map("drug.raw" -> ctx.value.id),
            ctx.arg(pageArg))
        }
      ),

      Field("adverseEvents", OptionType(adverseEventsImp),
        description = Some("The FDA Adverse Event Reporting System (FAERS)"),
        arguments = pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getAdverseEvents(
            Map("chembl_id.keyword" -> ctx.value.id),
            ctx.arg(pageArg)))
    ),
    ReplaceField("linkedDiseases", Field("linkedDiseases", linkedDiseasesImp, Some("Linked Diseases"),
      resolve = r => r.value.linkedDiseases)),
    ReplaceField("linkedTargets", Field("linkedTargets", linkedTargetsImp, Some("Linked Targets"),
      resolve = r => r.value.linkedTargets))
  )

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

  implicit val URLImp: ObjectType[Backend, URL] = deriveObjectType[Backend, URL]()
  implicit val knownDrugImp: ObjectType[Backend, KnownDrug] = deriveObjectType[Backend, KnownDrug](
    AddFields(
      Field("disease", OptionType(diseaseImp),
        resolve = r => diseasesFetcher.deferOpt(r.value.diseaseId)),
      Field("target", OptionType(targetImp),
        resolve = r => targetsFetcher.deferOpt(r.value.targetId)),
      Field("drug", OptionType(drugImp),
        resolve = r => drugsFetcher.deferOpt(r.value.drugId))
    )
  )

  implicit val knownDrugsImp: ObjectType[Backend, KnownDrugs] = deriveObjectType[Backend, KnownDrugs]()

}

object GQLSchema extends GQLMeta with GQLEntities {
  val resolvers = DeferredResolver.fetchers(targetsFetcher,
    drugsFetcher,
    diseasesFetcher,
    ecosFetcher,
    reactomeFetcher,
    expressionFetcher,
    mousePhenotypeFetcher)


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
