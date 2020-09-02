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
  val pageSize = Argument("size", OptionInputType(IntType))
  val cursor = Argument("cursor", OptionInputType(ListInputType(StringType)))
  val queryString = Argument("queryString", StringType, description = "Query string")
  val freeTextQuery = Argument("freeTextQuery", OptionInputType(StringType), description = "Query string")
  val efoId = Argument("efoId", StringType, description = "EFO ID" )
  val efoIds = Argument("efoIds", ListInputType(StringType), description = "EFO ID" )
  val networkExpansionId = Argument("networkExpansionId", OptionInputType(StringType), description = "Network expansion ID")
  val ensemblId = Argument("ensemblId", StringType, description = "Ensembl ID" )
  val ensemblIds = Argument("ensemblIds", ListInputType(StringType), description = "List of Ensembl IDs")
  val chemblId = Argument("chemblId", StringType, description = "Chembl ID" )
  val chemblIds = Argument("chemblIds", ListInputType(StringType), description = "List of Chembl IDs")
  val indrectEvidences = Argument("enableIndirect", OptionInputType(BooleanType),
    "Use disease ontology to capture evidences from all descendants to build associations")

  val BFilterString = Argument("BFilter", OptionInputType(StringType))
  val scoreSorting = Argument("orderByScore", StringType)
  val AId = Argument("A", StringType)
  val AIds = Argument("As", ListInputType(StringType))
  val BIds = Argument("Bs", ListInputType(StringType))
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

  implicit val datasourceSettingsJsonImp = Json.format[DatasourceSettings]
  implicit val datasourceSettingsInputImp = deriveInputObjectType[DatasourceSettings](
    InputObjectTypeName("DatasourceSettingsInput")
  )
  val datasourceSettingsListArg = Argument("datasources",
    OptionInputType(ListInputType(datasourceSettingsInputImp)))

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


  implicit val otarProjectsHasId = HasId[OtarProjects, String](_.efoId)

  val otarProjectsFetcherCache = FetcherCache.simple
  val otarProjectsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(entities.Configuration.batchSize).caching(otarProjectsFetcherCache),
    fetch = (ctx: Backend, ids: Seq[String]) => {
      ctx.getOtarProjects(ids)
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

  implicit val scoredDataTypeImp = deriveObjectType[Backend, ScoredComponent]()

  implicit val associatedOTFTargetImp = deriveObjectType[Backend, AssociationOTF](
    ObjectTypeName("AssociatedOTFTarget"),
    ObjectTypeDescription("Associated Target Entity"),
    ReplaceField("id", Field("target",
      targetImp, Some("Target"),
      resolve = r => targetsFetcher.defer(r.value.id)))
  )

  implicit val associatedOTFDiseaseImp = deriveObjectType[Backend, AssociationOTF](
    ObjectTypeName("AssociatedOTFDisease"),
    ObjectTypeDescription("Associated Disease Entity"),
    ReplaceField("id", Field("disease",
      diseaseImp, Some("Disease"),
      resolve = r => diseasesFetcher.defer(r.value.id)))
  )


  implicit val associatedTargetImp = deriveObjectType[Backend, Association](
    ObjectTypeName("AssociatedTarget"),
    ObjectTypeDescription("Associated Target Entity"),
    ReplaceField("id", Field("target",
      targetImp, Some("Target"),
      resolve = r => targetsFetcher.defer(r.value.id)))
  )

  implicit val associatedDiseaseImp = deriveObjectType[Backend, Association](
    ObjectTypeName("AssociatedDisease"),
    ObjectTypeDescription("Associated Disease Entity"),
    ReplaceField("id", Field("disease",
      diseaseImp, Some("Disease"),
      resolve = r => diseasesFetcher.defer(r.value.id)))
  )

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

  implicit val ecoImp = deriveObjectType[Backend, ECO](
    ObjectTypeDescription("Evidence & Conclusion Ontology (ECO) annotation"),

    DocumentField("id", "ECO term id"),
    DocumentField("label", "ECO term label")
  )

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

  implicit val tissueImp = deriveObjectType[Backend, Tissue](
    ObjectTypeDescription("Tissue, organ and anatomical system"),
    DocumentField("id", "UBERON id"),
    DocumentField("label", "UBERON tissue label"),
    DocumentField("anatomicalSystems", "Anatomical systems membership"),
    DocumentField("organs", "Organs membership"),
  )
  implicit val rnaExpressionImp = deriveObjectType[Backend, RNAExpression]()
  implicit val cellTypeImp = deriveObjectType[Backend, CellType]()
  implicit val proteinExpressionImp = deriveObjectType[Backend, ProteinExpression]()
  implicit val expressionImp = deriveObjectType[Backend, Expression]()
  implicit val expressionsImp = deriveObjectType[Backend, Expressions](
    ExcludeFields("id")
  )

  implicit val adverseEventImp = deriveObjectType[Backend, AdverseEvent](
    ObjectTypeDescription("Significant adverse event entries"),
    DocumentField("name", "Meddra term on adverse event"),
    DocumentField("count", "Number of reports mentioning drug and adverse event"),
    DocumentField("llr", "Log-likelihood ratio"),
    ExcludeFields("criticalValue")
  )

  implicit val adverseEventsImp = deriveObjectType[Backend, AdverseEvents](
    ObjectTypeDescription("Significant adverse events inferred from FAERS reports"),
    DocumentField("count", "Total significant adverse events"),
    DocumentField("critVal", "LLR critical value to define significance"),
    DocumentField("rows", "Significant adverse event entries")
  )

  implicit val otarProjectImp = deriveObjectType[Backend, OtarProject]()
  implicit val otarProjectsImp = deriveObjectType[Backend, OtarProjects]()

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

  implicit val sourceLinkImp = deriveObjectType[Backend, SourceLink](
    ObjectTypeDescription("\"Datasource link\""),

    DocumentField("source", "Source name"),
    DocumentField("link", "Source full url")
  )
  implicit val portalProbeImp = deriveObjectType[Backend, PortalProbe](
    ObjectTypeDescription("Chemical Probe entries (excluding Probeminer)"),

    DocumentField("note", "Additional note"),
    DocumentField("chemicalprobe", "Chemical probe name"),
    DocumentField("gene", "Chemical probe target as reported by source"),
    DocumentField("sourcelinks", "Sources")
  )

  implicit val chemicalProbesImp = deriveObjectType[Backend, ChemicalProbes](
    ObjectTypeDescription("Set of potent, selective and cell-permeable chemical probes"),

    DocumentField("probeminer", "Probeminer chemical probe url"),
    DocumentField("rows", "Chemical probes entries in SGC or ChemicalProbes.org")
  )

  implicit val experimentDetailsImp = deriveObjectType[Backend, ExperimentDetails]()
  implicit val experimentalToxicityImp = deriveObjectType[Backend, ExperimentalToxicity]()
  implicit val safetyCodeImp = deriveObjectType[Backend, SafetyCode]()
  implicit val safetyReferenceImp = deriveObjectType[Backend, SafetyReference]()
  implicit val adverseEffectsActivationEffectsImp = deriveObjectType[Backend, AdverseEffectsActivationEffects]()
  implicit val adverseEffectsInhibitionEffectsImp = deriveObjectType[Backend, AdverseEffectsInhibitionEffects]()
  implicit val adverseEffectsImp = deriveObjectType[Backend, AdverseEffects](
    ObjectTypeDescription("Curated target safety effects")
  )
  implicit val safetyRiskInfoImp = deriveObjectType[Backend, SafetyRiskInfo]()
  implicit val safetyImp = deriveObjectType[Backend, Safety]()

  implicit val genotypePhenotypeImp = deriveObjectType[Backend, GenotypePhenotype]()
  implicit val mousePhenotypeImp = deriveObjectType[Backend, MousePhenotype]()
  implicit val mouseGeneImp = deriveObjectType[Backend, MouseGene]()
  implicit val mousePhenotypesImp = deriveObjectType[Backend, MousePhenotypes]()

  implicit val tepImp = deriveObjectType[Backend, Tep](
    ObjectTypeDescription("Target Enabling Package (TEP)")
  )

  implicit val proteinClassPathNodeImp = deriveObjectType[Backend, ProteinClassPathNode]()
  implicit val proteinClassPathImp = deriveObjectType[Backend, ProteinClassPath]()

  implicit val proteinImp = deriveObjectType[Backend, ProteinAnnotations](
    ObjectTypeDescription("Various protein coding annotation derived from Uniprot"),

    DocumentField("id", "Uniprot reference accession"),
    DocumentField("accessions", "All accessions"),
    DocumentField("functions", "Protein function"),
    DocumentField("pathways", "Pathway membership"),
    DocumentField("similarities", "Protein similarities (families, etc.)"),
    DocumentField("subcellularLocations", "Subcellular locations"),
    DocumentField("subunits", "Protein subunits")
  )

  implicit val genomicLocationImp = deriveObjectType[Backend, GenomicLocation]()
  implicit lazy val targetImp: ObjectType[Backend, Target] = deriveObjectType(
    ObjectTypeDescription("Target entity"),
    DocumentField("id", "Open Targets target id"),
    DocumentField("approvedSymbol", "HGNC approved symbol"),
    DocumentField("approvedName", "Approved gene name"),
    DocumentField("bioType", "Molecule biotype"),
    DocumentField("hgncId", "HGNC approved id"),
    DocumentField("nameSynonyms", "Gene name synonyms"),
    DocumentField("symbolSynonyms", "Symbol synonyms"),
    DocumentField("genomicLocation", "Chromosomic location"),
    DocumentField("proteinAnnotations", "Various protein coding annotation"),
    DocumentField("geneOntology", "Gene Ontology annotations"),
    DocumentField("safety", "Known target safety effects and target safety risk information"),
    DocumentField("chemicalProbes", "Potent, selective and cell-permeable chemical probes"),
    DocumentField("hallmarks", "Target-modulated essential alterations in cell physiology that dictate " +
      "malignant growth"),
    DocumentField("tep", "Target Enabling Package (TEP)"),
    DocumentField("tractability", "Target druggability assessment"),
    DocumentField("reactome", "Biological pathway membership from Reactome"),

    ReplaceField("reactome", Field("reactome",
      ListType(reactomeImp), None,
      resolve = r => reactomeFetcher.deferSeq(r.value.reactome))),
    AddFields(
      Field("mousePhenotypes", ListType(mouseGeneImp),
        description = Some("Biological pathway membership from Reactome"),
        resolve = r => DeferredValue(mousePhenotypeFetcher.deferOpt(r.value.id)).map {
          case Some(mouseGenes) => mouseGenes.rows
          case None => Seq.empty
        }),
      Field("expressions", ListType(expressionImp),
        description = Some("RNA and Protein baseline expression"),
        resolve = r => DeferredValue(expressionFetcher.deferOpt(r.value.id)).map {
          case Some(expressions) => expressions.rows
          case None => Seq.empty
        }),
      Field("knownDrugs", OptionType(knownDrugsImp),
        description = Some("Clinical precedence for drugs with investigational or approved indications " +
          "targeting gene products according to their curated mechanism of action"),
        arguments = freeTextQuery :: pageSize :: cursor :: Nil,
        resolve = ctx => {
          ctx.ctx.getKnownDrugs(ctx.arg(freeTextQuery).getOrElse(""),
            Map("target.raw" -> ctx.value.id),
            ctx.arg(pageSize),
            ctx.arg(cursor).getOrElse(Nil)
          )
        }
      ),
      Field("cancerBiomarkers", OptionType(cancerBiomarkersImp),
        description = Some("Clinical relevance and drug responses of tumor genomic alterations " +
          "on the target"),
        arguments = pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getCancerBiomarkers(
            Map("target.keyword" -> ctx.value.id),
            ctx.arg(pageArg))),

      Field("relatedTargets", OptionType(relatedTargetsImp),
        description = Some("Similar targets based on their disease association profiles"),
        arguments = pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getRelatedTargets(
            Map("A.keyword" -> ctx.value.id),
            ctx.arg(pageArg))),

      Field("associatedDiseases", ListType(associatedDiseaseImp),
        description = Some("Ranked list of diseases associated to this target"),
        arguments = indrectEvidences :: freeTextQuery :: pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getAssociationsByTarget(ctx.value.id, ctx.arg(indrectEvidences).getOrElse(false) ,ctx.arg(freeTextQuery), ctx.arg(pageArg)))

//      Field("associationsOnTheFly", associationsImp,
//        description = Some("Associations for a fixed target"),
//        arguments = datasourceSettingsListArg :: networkExpansionId :: pageArg :: Nil,
//        resolve = ctx =>
//          ctx.ctx.getAssociationsTargetFixed(ctx.value.id,
//            ctx.arg(datasourceSettingsListArg),
//            ctx.arg(networkExpansionId),
//            ctx.arg(pageArg)))
  ))

  implicit val phenotypeImp = deriveObjectType[Backend, Phenotype](
    ObjectTypeDescription("Clinical signs and symptoms observed in disease"),

    DocumentField("url", "Disease or phenotype uri"),
    DocumentField("name", "Disease or phenotype name"),
    DocumentField("disease", "Disease or phenotype id")
  )
  // disease
  implicit lazy val diseaseImp: ObjectType[Backend, Disease] = deriveObjectType(
    ObjectTypeDescription("Disease or phenotype entity"),
    DocumentField("id", "Open Targets disease id"),
    DocumentField("name", "Disease name"),
    DocumentField("description", "Disease description"),
    DocumentField("synonyms", "Disease synonyms"),
    DocumentField("phenotypes", "Clinical signs and symptoms observed in disease"),
    DocumentField("isTherapeuticArea", "Is disease a therapeutic area itself"),

    ReplaceField("therapeuticAreas", Field("therapeuticAreas",
      ListType(diseaseImp), Some("Ancestor therapeutic area disease entities in ontology"),
      resolve = r => diseasesFetcher.deferSeq(r.value.therapeuticAreas))),
//    ReplaceField("phenotypes", Field("phenotypes",
//      ListType(diseaseImp), Some("Phenotype List"),
//      resolve = r => diseasesFetcher.deferSeq(r.value.phenotypes))),
    ReplaceField("parents", Field("parents",
      ListType(diseaseImp), Some("Disease parents entities in ontology"),
      resolve = r => diseasesFetcher.deferSeq(r.value.parents))),
    ReplaceField("children", Field("children",
      ListType(diseaseImp), Some("Disease children entities in ontology"),
      resolve = r => diseasesFetcher.deferSeq(r.value.children))),
    // this query uses id and ancestors fields to search for indirect diseases
    AddFields(
      Field("otarProjects", ListType(otarProjectImp),
        description = Some("RNA and Protein baseline expression"),
        resolve = r => DeferredValue(otarProjectsFetcher.deferOpt(r.value.id)).map {
          case Some(otars) => otars.rows
          case None => Seq.empty
        }),
      Field("knownDrugs", OptionType(knownDrugsImp),
        description = Some("Clinical precedence for investigational or approved " +
          "drugs indicated for disease and curated mechanism of action"),
        arguments = freeTextQuery :: pageSize :: cursor :: Nil,
        resolve = ctx => {
          ctx.ctx.getKnownDrugs(
            ctx.arg(freeTextQuery).getOrElse(""),
            Map(
              "disease.raw" -> ctx.value.id,
              "ancestors.raw" -> ctx.value.id
            ),
            ctx.arg(pageSize),
            ctx.arg(cursor).getOrElse(Nil)
          )
        }
      ),

      Field("relatedDiseases", OptionType(relatedDiseasesImp),
        description = Some("Similar diseases based on their target association profiles"),
        arguments = pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getRelatedDiseases(
            Map("A.keyword" -> ctx.value.id),
            ctx.arg(pageArg))),
      Field("associatedTargets", ListType(associatedTargetImp),
        description = Some("Ranked list of targets associated to this disease"),
        arguments = indrectEvidences :: freeTextQuery :: pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getAssociationsByDisease(ctx.value.id, ctx.arg(indrectEvidences).getOrElse(true), ctx.arg(freeTextQuery), ctx.arg(pageArg)))

//      Field("associationsOnTheFly", associationsImp,
//        description = Some("Associations for a fixed disease"),
//        arguments = datasourceSettingsListArg :: networkExpansionId :: pageArg :: Nil,
//        resolve = ctx =>
//          ctx.ctx.getAssociationsDiseaseFixed(ctx.value.id,
//            ctx.arg(datasourceSettingsListArg),
//            ctx.arg(networkExpansionId),
//            ctx.arg(pageArg)))
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
  implicit val cancerBiomarkerSourceImp = deriveObjectType[Backend, CancerBiomarkerSource](
    ObjectTypeDescription("Detail on Cancer Biomarker sources"),

    DocumentField("description", "Source description"),
    DocumentField("link", "Source link"),
    DocumentField("name", "Source name")
  )

  implicit val cancerBiomarkerImp = deriveObjectType[Backend, CancerBiomarker](
    ObjectTypeDescription("Entry on clinical relevance and drug responses of tumor genomic " +
      "alterations on the target"),

    DocumentField("id", "Target symbol and variant id"),
    DocumentField("associationType", "Drug responsiveness"),
    DocumentField("drugName", "Drug family or name"),
    DocumentField("evidenceLevel", "Source type"),
    DocumentField("sources", "Sources"),
    DocumentField("pubmedIds", "List of supporting publications"),
    DocumentField("evidenceLevel", "Source type"),

    ReplaceField("target", Field("target", targetImp, Some("Target entity"),
      resolve = r => targetsFetcher.defer(r.value.target))),
    ReplaceField("disease", Field("disease", OptionType(diseaseImp), Some("Disease entity"),
      resolve = r => diseasesFetcher.deferOpt(r.value.disease)))
  )

  implicit val cancerBiomarkersImp = deriveObjectType[Backend, CancerBiomarkers](
    ObjectTypeDescription("Set of clinical relevance and drug responses of tumor " +
      "genomic alterations on the target entries"),

    DocumentField("uniqueDrugs", "Number of unique drugs with response information"),
    DocumentField("uniqueDiseases", "Number of unique cancer diseases with drug response information"),
    DocumentField("uniqueBiomarkers", "Number of unique biomarkers with drug response information"),
    DocumentField("count", "Number of entries"),
    DocumentField("rows", "Cancer Biomarker entries")
  )

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
  implicit lazy val withdrawnNoticeImp = deriveObjectType[Backend, WithdrawnNotice](
    ObjectTypeDescription("Withdrawal reason"),

    DocumentField("classes", "Withdrawal classes"),
    DocumentField("countries", "Withdrawal countries"),
    DocumentField("reasons", "Reason for withdrawal"),
    DocumentField("year", "Year of withdrawal")
  )
  implicit lazy val drugImp = deriveObjectType[Backend, Drug](
    ObjectTypeDescription("Drug/Molecule entity"),
    DocumentField("id", "Open Targets molecule id"),
    DocumentField("name", "Molecule preferred name"),
    DocumentField("synonyms", "Molecule synonyms"),
    DocumentField("tradeNames", "Drug trade names"),
    DocumentField("yearOfFirstApproval", "Year drug was approved for the first time"),
    DocumentField("drugType", "Drug modality"),
    DocumentField("maximumClinicalTrialPhase", "Maximum phase observed in clinical trial records and" +
      " post-marketing package inserts"),
    DocumentField("hasBeenWithdrawn", "Has drug been withdrawn from the market"),
    DocumentField("withdrawnNotice", "Withdrawal reason"),
    DocumentField("internalCompound", "Is this an private molecule not displayed " +
      "in the Open Targets public version"),
    DocumentField("mechanismsOfAction", "Mechanisms of action to produce intended " +
      "pharmacological effects. Curated from scientific literature and post-marketing package inserts"),
    DocumentField("indications", "Investigational and approved indications curated from clinical trial " +
      "records and post-marketing package inserts"),
    DocumentField("blackBoxWarning", "Alert on life-threteaning drug side effects provided by FDA"),
    DocumentField("description", "Drug description"),

    AddFields(
      Field("knownDrugs", OptionType(knownDrugsImp),
        description = Some("Curated Clinical trial records and and post-marketing package inserts " +
          "with a known mechanism of action"),
        arguments = freeTextQuery :: pageSize :: cursor :: Nil,
        resolve = ctx => {
          ctx.ctx.getKnownDrugs(
            ctx.arg(freeTextQuery).getOrElse(""),
            Map("drug.raw" -> ctx.value.id),
            ctx.arg(pageSize),
            ctx.arg(cursor).getOrElse(Nil)
          )
        }
      ),

      Field("adverseEvents", OptionType(adverseEventsImp),
        description = Some("Significant adverse events inferred from FAERS reports"),
        arguments = pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getAdverseEvents(
            Map("chembl_id.keyword" -> ctx.value.id),
            ctx.arg(pageArg)))
    ),
    ReplaceField("linkedDiseases", Field("linkedDiseases", linkedDiseasesImp,
      Some("Therapeutic indications for drug based on clinical trial data or " +
        "post-marketed drugs, when mechanism of action is known\""),
      resolve = r => r.value.linkedDiseases)),
    ReplaceField("linkedTargets", Field("linkedTargets", linkedTargetsImp,
      Some("Molecule targets based on drug mechanism of action"),
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
//  implicit lazy val associationsImp = deriveObjectType[Backend, Associations]()

  implicit val evidenceSourceImp = deriveObjectType[Backend, EvidenceSource]()
  implicit val associatedOTFTargetsImp = deriveObjectType[Backend, AssociationsOTF](
    ObjectTypeName("AssociatedTargetsOTF"),
    ReplaceField("rows", Field("rows",
      ListType(associatedOTFTargetImp), Some("Associated Targets using (On the fly method)"),
      resolve = r => r.value.rows))
  )

  implicit val associatedOTFDiseasesImp = deriveObjectType[Backend, AssociationsOTF](
    ObjectTypeName("AssociatedDiseasesOTF"),
    ReplaceField("rows", Field("rows",
      ListType(associatedOTFDiseaseImp), Some("Associated Targets using (On the fly method)"),
      resolve = r => r.value.rows))
  )

  // implement associations
//  val associationsObTheFlyGQLImp = ObjectType("AssociationsOnTheFly",
//    "Compute Associations on the fly",
//    fields[Backend, Unit](
//      Field("meta", clickhouseSettingsImp,
//        None,
//        resolve = _.ctx.defaultOTSettings.clickhouse),
//      Field("byTargetFixed", associationsImp,
//        description = Some("Associations for a fixed target"),
//        arguments = ensemblId :: datasourceSettingsListArg :: networkExpansionId :: pageArg :: Nil,
//        resolve = ctx =>
//          ctx.ctx.getAssociationsTargetFixed(ctx.arg(ensemblId),
//            ctx.arg(datasourceSettingsListArg),
//            ctx.arg(networkExpansionId),
//            ctx.arg(pageArg))),
//      Field("byDiseaseFixed", associationsImp,
//        description = Some("Associations for a fixed disease"),
//        arguments = efoId :: datasourceSettingsListArg :: networkExpansionId :: pageArg :: Nil,
//        resolve = ctx => ctx.ctx.getAssociationsDiseaseFixed(ctx.arg(efoId),
//          ctx.arg(datasourceSettingsListArg),
//          ctx.arg(networkExpansionId),
//          ctx.arg(pageArg)))
//    ))

  implicit val URLImp: ObjectType[Backend, URL] = deriveObjectType[Backend, URL](
    ObjectTypeDescription("Source URL for clinical trials, FDA and package inserts"),

    DocumentField("url", "resource url"),
    DocumentField("name", "resource name")
  )
  implicit val knownDrugImp: ObjectType[Backend, KnownDrug] = deriveObjectType[Backend, KnownDrug](
    ObjectTypeDescription("Clinical precedence entry for drugs with investigational or " +
      "approved indications targeting gene products according to their curated mechanism of " +
      "action. Entries are grouped by target, disease, drug, phase, status and mechanism of action"),
    DocumentField("approvedSymbol", "Drug target approved symbol based on curated mechanism of action"),
    DocumentField("label", "Curated disease indication"),
    DocumentField("prefName", "Drug name"),
    DocumentField("drugType", "Drug modality"),
    DocumentField("targetId", "Drug target Open Targets id based on curated mechanism of action"),
    DocumentField("diseaseId", "Curated disease indication Open Targets id"),
    DocumentField("drugId", "Open Targets drug id"),
    DocumentField("phase", "Clinical Trial phase"),
    DocumentField("mechanismOfAction", "Mechanism of Action description"),
    DocumentField("status", "Trial status"),
    DocumentField("activity", "On-target drug pharmacological activity"),
    DocumentField("targetClass", "Drug target class based on curated mechanism of action"),
    DocumentField("ctIds", "Clinicaltrials.gov identifiers on entry trials"),
    DocumentField("urls", "Source urls from clinical trials, FDA or package inserts"),

    AddFields(
      Field("disease", OptionType(diseaseImp),
        description = Some("Curated disease indication entity"),
        resolve = r => diseasesFetcher.deferOpt(r.value.diseaseId)),
      Field("target", OptionType(targetImp),
        description = Some("Drug target entity based on curated mechanism of action"),
        resolve = r => targetsFetcher.deferOpt(r.value.targetId)),
      Field("drug", OptionType(drugImp),
        description = Some("Curated drug entity"),
        resolve = r => drugsFetcher.deferOpt(r.value.drugId))
    )
  )

  implicit val knownDrugsImp: ObjectType[Backend, KnownDrugs] = deriveObjectType[Backend, KnownDrugs](
    ObjectTypeDescription("Set of clinical precedence for drugs with investigational or " +
      "approved indications targeting gene products according to their curated mechanism of action"),

    DocumentField("uniqueDrugs", "Total unique drugs/molecules"),
    DocumentField("uniqueDiseases", "Total unique diseases or phenotypes"),
    DocumentField("uniqueTargets", "Total unique known mechanism of action targetsTotal " +
      "unique known mechanism of action targets"),
    DocumentField("count", "Total number of entries"),
    DocumentField("rows", "Clinical precedence entries with known mechanism of action")
  )

}

object GQLSchema extends GQLMeta with GQLEntities {
  val resolvers = DeferredResolver.fetchers(targetsFetcher,
    drugsFetcher,
    diseasesFetcher,
    ecosFetcher,
    reactomeFetcher,
    expressionFetcher,
    mousePhenotypeFetcher,
    otarProjectsFetcher)


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

      Field("aotfByDisease", associatedOTFTargetsImp,
        description = Some("associations on the fly"),
        arguments = AId :: indrectEvidences :: BFilterString :: pageArg :: Nil,
        resolve = ctx => ctx.ctx.getAssociationsDiseaseFixed(
          ctx arg AId,
          None,
          ctx arg indrectEvidences getOrElse(true),
          ctx arg BFilterString,
          ctx arg pageArg
        )),

      Field("aotfByTarget", associatedOTFDiseasesImp,
        description = Some("associations on the fly"),
        arguments = AId :: indrectEvidences :: BFilterString :: pageArg :: Nil,
        resolve = ctx => ctx.ctx.getAssociationsTargetFixed(
          ctx arg AId,
          None,
          ctx arg indrectEvidences getOrElse(false),
          ctx arg BFilterString,
          ctx arg pageArg
        )),

      Field("associationDatasources", ListType(evidenceSourceImp),
        description = Some("The complete list of all possible datasources"),
        resolve = ctx => ctx.ctx.getAssociationDatasources)
    ))

  val schema = Schema(query)
}
