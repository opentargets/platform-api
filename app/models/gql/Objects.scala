package models.gql

import models.*
import models.entities.Configuration.*
import models.entities.Evidences.*
import models.entities.Interactions.*
import models.entities.Publications.publicationsImp
import models.entities.Colocalisations.*
import models.entities.*
import models.gql.Arguments.*
import models.gql.Fetchers.*
import models.Helpers.ComplexityCalculator.*
import models.entities.ClinicalIndications.{
  clinicalIndicationsFromDiseaseImp,
  clinicalIndicationsFromDrugImp
}
import play.api.libs.json.*
import sangria.macros.derive.{DocumentField, *}
import sangria.schema.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.*
import models.entities.CredibleSets.credibleSetsImp
import models.entities.Study.{LdPopulationStructure, Sample, SumStatQC}
import models.entities.Violations.{
  InputParameterCheckError,
  InvalidArgValueError,
  invalidArgValueErrorMsg
}

import scala.collection.View.Empty
import net.logstash.logback.argument.StructuredArguments.keyValue
import utils.OTLogging

object Objects extends OTLogging {

  implicit val metaDataVersionImp: ObjectType[Backend, DataVersion] =
    deriveObjectType[Backend, DataVersion](
      ObjectTypeDescription("Data release version information"),
      DocumentField("year", "Year of the Platform data release"),
      DocumentField("month", "Month of the Platform data release"),
      DocumentField("iteration",
                    "Iteration number of the Platform data release within the year-month period"
      )
    )
  implicit val metaAPIVersionImp: ObjectType[Backend, APIVersion] =
    deriveObjectType[Backend, APIVersion](
      ObjectTypeDescription("API version information"),
      DocumentField("x", "Major version number"),
      DocumentField("y", "Minor version number"),
      DocumentField("z", "Patch version number"),
      DocumentField("suffix", "Optional version suffix (e.g., alpha, beta, rc)")
    )
  implicit val metaImp: ObjectType[Backend, Meta] = deriveObjectType[Backend, Meta](
    ObjectTypeDescription(
      "Metadata about the Open Targets Platform API including version information"
    ),
    DocumentField("name", "Name of the platform"),
    DocumentField("apiVersion", "API version information"),
    DocumentField("dataVersion", "Data release version information"),
    DocumentField("product", "Open Targets product"),
    DocumentField("enableDataReleasePrefix",
                  "Flag indicating whether data release prefix is enabled"
    ),
    DocumentField("dataPrefix", "Data release prefix"),
    AddFields(
      Field(
        "downloads",
        OptionType(StringType),
        description = Some(
          "Platform datasets described following MLCroissant metadata format. Datasets are described in a JSONLD file containing extensive metadata including table and column descriptions, schemas, location and relationships."
        ),
        resolve = _.ctx.getDownloads
      )
    )
  )

  // Define a case class to represent each item in the array
  case class KeyValue(key: String, value: BigDecimal)

  implicit val KeyValueFormat: OFormat[KeyValue] = Json.format[KeyValue]

  implicit val geneEssentialityScreenImp: ObjectType[Backend, GeneEssentialityScreen] =
    deriveObjectType[Backend, GeneEssentialityScreen](
      ObjectTypeDescription(
        "CRISPR screening experiments supporting the essentiality assessment. Represents individual cell line assays from DepMap."
      ),
      DocumentField("cellLineName",
                    "Name of the cancer cell line in which the gene essentiality was assessed"
      ),
      DocumentField("depmapId", "Unique identifier of the assay in DepMap"),
      DocumentField("diseaseCellLineId",
                    "Cell model passport identifier of a cell line modelling a disease"
      ),
      DocumentField("diseaseFromSource",
                    "Disease associated with the cell line as reported in the source data"
      ),
      DocumentField("expression", "Gene expression level in the corresponding cell line"),
      DocumentField("geneEffect", "Gene effect score indicating the impact of gene knockout"),
      DocumentField("mutation", "Background mutation the tested cell line have")
    )

  implicit val depMapEssentialityImp: ObjectType[Backend, DepMapEssentiality] =
    deriveObjectType[Backend, DepMapEssentiality](
      ObjectTypeDescription(
        "Essentiality measurements extracted from DepMap, stratified by tissue or anatomical units. Gene effects below -1 can be considered dependencies."
      ),
      DocumentField("screens",
                    "List of CRISPR screening experiments supporting the essentiality assessment"
      ),
      DocumentField(
        "tissueId",
        "Identifier of the tissue from where the cells were sampled for assay [bioregistry:uberon]"
      ),
      DocumentField("tissueName", "Name of the tissue from where the cells were sampled for assay")
    )

  val KeyValueObjectType: ObjectType[Unit, KeyValue] = ObjectType(
    "KeyValue",
    "A key-value pair",
    fields[Unit, KeyValue](
      Field("key", StringType, description = Some("Key or attribute name"), resolve = _.value.key),
      Field("value",
            StringType,
            description = Some("String representation of the value"),
            resolve = _.value.value.toString()
      )
    )
  )

  // Define the ObjectType for the array
  val KeyValueArrayObjectType: ObjectType[Unit, JsArray] = ObjectType(
    "KeyValueArray",
    "An array of key-value pairs",
    fields[Unit, JsArray](
      Field("items",
            ListType(KeyValueObjectType),
            description = Some("List of key-value entries"),
            resolve = _.value.as[List[KeyValue]]
      )
    )
  )

  implicit lazy val targetImp: ObjectType[Backend, Target] = deriveObjectType(
    ObjectTypeDescription(
      "Core annotation for drug targets (gene/proteins). Targets are defined based on EMBL-EBI Ensembl database and uses the Ensembl gene ID as the  primary identifier. An Ensembl gene ID is considered potential drug target if included in the canonical assembly or if present alternative assemblies but encoding for a reviewed protein product according to the UniProt database."
    ),
    DocumentField("id", "Unique identifier for the target [bioregistry:ensembl]"),
    DocumentField("approvedSymbol", "Approved gene symbol of the target"),
    DocumentField("approvedName", "Approved full name of the target gene"),
    DocumentField(
      "biotype",
      "Biotype classification of the target gene, indicating if the gene is protein coding"
    ),
    DocumentField("canonicalTranscript", "The Ensembl canonical transcript of the target gene"),
    DocumentField("alternativeGenes",
                  "List of alternative Ensembl gene identifiers mapped to non-canonical chromosomes"
    ),
    DocumentField("targetClass", "Target classification categories from ChEMBL"),
    DocumentField("chemicalProbes",
                  "Chemical probes with high selectivity and specificity for the target."
    ),
    DocumentField("dbXrefs", "Database cross-references for the target"),
    DocumentField("functionDescriptions",
                  "Functional descriptions of the target gene sourced from UniProt"
    ),
    DocumentField(
      "constraint",
      "Constraint scores for the target gene from GnomAD based on loss-of-function intolerance."
    ),
    DocumentField("genomicLocation", "Genomic location information of the target gene"),
    DocumentField("go", "List of Gene Ontology (GO) annotations related to the target"),
    DocumentField(
      "hallmarks",
      "Hallmarks related to the target gene sourced from COSMIC"
    ),
    DocumentField("homologues", "Homologues of the target gene in other species"),
    DocumentField("proteinIds", "Protein identifiers associated with the target"),
    DocumentField(
      "safetyLiabilities",
      "Known target safety effects and target safety risk information"
    ),
    DocumentField("subcellularLocations",
                  "List of subcellular locations where the target protein is found"
    ),
    DocumentField("synonyms", "List of synonyms for the target gene"),
    DocumentField("obsoleteSymbols",
                  "List of obsolete symbols previously used for the target gene"
    ),
    DocumentField("obsoleteNames", "List of obsolete names previously used for the target gene"),
    DocumentField("nameSynonyms", "List of name-based synonyms for the target gene"),
    DocumentField("symbolSynonyms", "List of symbol-based synonyms for the target gene"),
    DocumentField("tep", "Target Enabling Package (TEP) information"),
    DocumentField("tractability", "Tractability information for the target"),
    DocumentField("transcriptIds",
                  "List of Ensembl transcript identifiers associated with the target"
    ),
    DocumentField("pathways", "Pathway annotations for the target"),
    RenameField("go", "geneOntology"),
    RenameField("constraint", "geneticConstraint"),
    ReplaceField(
      "studyLocusIds",
      Field(
        "credibleSets",
        credibleSetsImp,
        description = Some(
          "95% credible sets for GWAS and molQTL studies. Credible sets include all variants in the credible set as well as the fine-mapping method and statistics used to estimate the credible set."
        ),
        arguments = pageArg :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = ctx =>
          if (ctx.value.studyLocusIds.isEmpty) {
            Future.successful(CredibleSets.empty)
          } else {
            val credSetQueryArgs = CredibleSetQueryArgs(
              ctx.value.studyLocusIds
            )
            ctx.ctx.getCredibleSets(credSetQueryArgs, ctx.arg(pageArg))
          }
      )
    ),
    AddFields(
      Field(
        "similarEntities",
        ListType(similarityGQLImp),
        description = Some("Return similar labels using a model Word2CVec trained with PubMed"),
        arguments = idsArg :: entityNames :: thresholdArg :: pageSize :: Nil,
        complexity = Some(complexityCalculator(pageSize)),
        resolve = c => {
          val ids = c.arg(idsArg).getOrElse(List.empty)
          val thres = c.arg(thresholdArg).getOrElse(0.1)
          val cats = c.arg(entityNames).getOrElse(Nil).toList
          val n = c.arg(pageSize).getOrElse(10)

          c.ctx.getSimilarW2VEntities(c.value.id, ids.toSet, cats, thres, n)
        }
      ),
      Field(
        "literatureOcurrences",
        publicationsImp,
        description = Some(
          "Return the list of publications that mention the main entity, " +
            "alone or in combination with other entities"
        ),
        arguments = idsArg :: startYear :: startMonth :: endYear :: endMonth :: cursor :: Nil,
        resolve = c => {
          val ids = c.arg(idsArg).getOrElse(List.empty) ++ List(c.value.id)
          val filterStartYear = c.arg(startYear)
          val filterStartMonth = c.arg(startMonth)
          val filterEndYear = c.arg(endYear)
          val filterEndMonth = c.arg(endMonth)
          val cur = c.arg(cursor)

          c.ctx.getLiteratureOcurrences(
            ids.toSet,
            filterStartYear,
            filterStartMonth,
            filterEndYear,
            filterEndMonth,
            cur
          )
        }
      ),
      Field(
        "evidences",
        evidencesImp,
        description = Some(
          "Target-disease evidence from all data sources supporting associations between this target and diseases or phenotypes. Evidence entries are reported and scored according to confidence in the association."
        ),
        arguments = efoIds :: datasourceIdsArg :: pageSize :: cursor :: Nil,
        complexity = Some(complexityCalculator(pageSize)),
        resolve = ctx =>
          ctx.ctx.getEvidencesByEfoId(
            ctx arg datasourceIdsArg,
            Seq(ctx.value.id),
            ctx arg efoIds,
            Some(("score", "desc")),
            ctx arg pageSize,
            ctx arg cursor
          )
      ),
      Field(
        "interactions",
        OptionType(interactions),
        description = Some(
          "Molecular interactions reporting experimental or functional interactions between this target and other molecules. Interactions are integrated from multiple databases capturing physical interactions (e.g., IntAct), directional interactions (e.g., Signor), pathway relationships (e.g., Reactome), or functional interactions (e.g., STRINGdb)."
        ),
        arguments = scoreThreshold :: databaseName :: pageArg :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = r => {
          import r.ctx._

          Interactions.find(r.value.id, r arg scoreThreshold, r arg databaseName, r arg pageArg)
        }
      ),
      Field(
        "mousePhenotypes",
        ListType(mousePhenotypeImp),
        description = Some(
          "Mouse phenotype information linking this human target to observed phenotypes in mouse models. Provides data on phenotypes observed when the target gene is modified in mouse models."
        ),
        resolve = ctx => {
          val mp = ctx.ctx.getMousePhenotypes(Seq(ctx.value.id))
          mp
        }
      ),
      Field(
        "expressions",
        ListType(expressionImp),
        description = Some(
          "Baseline RNA and protein expression data across tissues for this target. Expression data shows how targets are selectively expressed across different tissues and biosamples, combining values from multiple sources including Expression Atlas and Human Protein Atlas."
        ),
        resolve = r =>
          DeferredValue(expressionFetcher.deferOpt(r.value.id)).map {
            case Some(expressions) => expressions.rows
            case None              => Seq.empty
          }
      ),
      Field(
        "associatedDiseases",
        associatedOTFDiseasesImp,
        description = Some(
          "Target-disease associations calculated on-the-fly using configurable data source weights and evidence filters. Returns associations with aggregated scores and evidence counts supporting the target-disease relationship."
        ),
        arguments =
          BIds :: indirectTargetEvidences :: datasourceSettingsListArg :: includeMeasurements :: facetFiltersListArg :: BFilterString :: scoreSorting :: pageArg :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = ctx => {
          (ctx arg datasourceSettingsListArg) foreach { settingsList =>
            val invalidWeights =
              for setting <- settingsList if setting.weight > 1 || setting.weight < 0
              yield s"The assigned weight for '${setting.id}' was ${setting.weight}"
            if invalidWeights.nonEmpty then
              val errors = Vector(
                invalidWeights.map(invWeight =>
                  InvalidArgValueError("weight", "between 0 and 1", Some(invWeight))
                )*
              )
              throw InputParameterCheckError(errors)
          }
          ctx.ctx.getAssociationsTargetFixed(
            ctx.value,
            ctx arg datasourceSettingsListArg,
            ctx arg indirectTargetEvidences getOrElse false,
            ctx arg includeMeasurements getOrElse false,
            ctx arg facetFiltersListArg getOrElse (Seq.empty),
            ctx arg BIds map (_.toSet) getOrElse Set.empty,
            ctx arg BFilterString,
            (ctx arg scoreSorting) map (_.split(" ").take(2).toList match {
              case a :: b :: Nil => (a, b)
              case a :: Nil      => (a, "desc")
              case _             => ("score", "desc")
            }),
            ctx arg pageArg
          )
        }
      ),
      Field(
        "prioritisation",
        OptionType(KeyValueArrayObjectType),
        description = Some(
          "Target-specific properties used to prioritise targets for further investigation. Prioritisation factors cover several areas around clinical precedence, tractability, do-ability, and safety of the target. Values range from -1 (unfavourable/deprioritised) to 1 (favourable/prioritised)."
        ),
        arguments = Nil,
        resolve = ctx => ctx.ctx.getTargetsPrioritisationJs(ctx.value.id)
      ),
      Field(
        "isEssential",
        OptionType(BooleanType),
        description = Some(
          "Flag indicating whether this target is essential based on CRISPR screening data from cancer cell line models. Essential genes are those that show dependency when knocked out in cellular models."
        ),
        resolve = ctx => {
          val mp = ctx.ctx.getTargetEssentiality(Seq(ctx.value.id))
          mp map { case ess =>
            if (ess.isEmpty) null else ess.head.geneEssentiality.head.isEssential
          }
        }
      ),
      Field(
        "depMapEssentiality",
        OptionType(ListType(depMapEssentialityImp)),
        description = Some(
          "Essentiality measurements extracted from DepMap, stratified by tissue or anatomical units. Gene essentiality is assessed based on dependencies exhibited when knocking out genes in cancer cellular models using CRISPR screenings from the Cancer Dependency Map (DepMap) Project. Gene effects below -1 can be considered dependencies."
        ),
        resolve = ctx => {
          val mp = ctx.ctx.getTargetEssentiality(Seq(ctx.value.id))
          mp map { case ess =>
            if (ess.isEmpty) null else ess.head.geneEssentiality.flatMap(_.depMapEssentiality)
          }
        }
      ),
      Field(
        "pharmacogenomics",
        ListType(pharmacogenomicsImp),
        description = Some(
          "Pharmacogenomics data linking genetic variants affecting this target to drug responses. Data is integrated from sources including ClinPGx and describes how genetic variants influence individual drug responses when targeting this gene product."
        ),
        arguments = pageArg :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = ctx => ctx.ctx.getPharmacogenomicsByTarget(ctx.value.id)
      ),
      Field(
        "proteinCodingCoordinates",
        proteinCodingCoordinatesImp,
        description = Some(
          "Protein coding coordinates linking variants affecting this target to their amino acid-level consequences in protein products. Describes variant consequences at the protein level including amino acid changes and their positions for this target."
        ),
        arguments = pageArg :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = ctx => ctx.ctx.getProteinCodingCoordinatesByTarget(ctx.value.id, ctx.arg(pageArg))
      )
    )
  )

  implicit lazy val chemicalProbeUrlImp: ObjectType[Backend, ChemicalProbeUrl] =
    deriveObjectType[Backend, ChemicalProbeUrl](
      ObjectTypeDescription("URL information for chemical probe resources"),
      DocumentField("niceName", "Nice name for the linked URL"),
      DocumentField("url", "URL providing details about the chemical probe")
    )
  implicit lazy val chemicalProbeImp: ObjectType[Backend, ChemicalProbe] =
    deriveObjectType[Backend, ChemicalProbe](
      ObjectTypeDescription(
        "Chemical probes related to the target. High-quality chemical probes are small molecules that can be used to modulate and study the function of proteins."
      ),
      DocumentField("id", "Unique identifier for the chemical probe"),
      DocumentField("control", "Whether the chemical probe serves as a control"),
      DocumentField("drugId", "Drug ID associated with the chemical probe"),
      DocumentField("mechanismOfAction", "Mechanism of action of the chemical probe"),
      DocumentField("isHighQuality", "Indicates if the chemical probe is high quality"),
      DocumentField("origin", "Origin of the chemical probe"),
      DocumentField("probeMinerScore", "Score from ProbeMiner for chemical probe quality"),
      DocumentField("probesDrugsScore", "Score for chemical probes related to druggability"),
      DocumentField("scoreInCells", "Score indicating chemical probe activity in cells"),
      DocumentField("scoreInOrganisms", "Score indicating chemical probe activity in organisms"),
      DocumentField("targetFromSourceId", "Ensembl gene ID of the target for the chemical probe"),
      DocumentField("urls", "URLs linking to more information about the chemical probe")
    )

  implicit lazy val reactomePathwayImp: ObjectType[Backend, ReactomePathway] =
    deriveObjectType[Backend, ReactomePathway](
      ObjectTypeDescription("Reactome pathway information for the target"),
      DocumentField("pathway", "Pathway name"),
      DocumentField("pathwayId", "Reactome pathway identifier"),
      DocumentField("topLevelTerm", "Top-level pathway term")
    )
  // disease
  implicit lazy val diseaseSynonymsImp: ObjectType[Backend, DiseaseSynonyms] =
    deriveObjectType[Backend, DiseaseSynonyms](
      ObjectTypeDescription("Synonymous disease labels grouped by relationship type"),
      DocumentField("relation", "Type of synonym relationship (e.g., exact, related, narrow)"),
      DocumentField("terms", "List of synonymous disease labels for this relationship type")
    )

  implicit lazy val diseaseImp: ObjectType[Backend, Disease] = deriveObjectType(
    ObjectTypeDescription(
      "Core annotation for diseases or phenotypes. A disease or phenotype in the Platform is understood as any disease, phenotype, biological process or measurement that might have any type of causality relationship with a human target. The EMBL-EBI Experimental Factor Ontology (EFO) (slim version) is used as scaffold for the disease or phenotype entity."
    ),
    DocumentField("id", "Open Targets disease identifier [bioregistry:efo]"),
    DocumentField("name", "Preferred disease or phenotype label"),
    DocumentField("description", "Short description of the disease or phenotype"),
    DocumentField("synonyms", "Synonymous disease or phenotype labels"),
    DocumentField("dbXRefs", "Cross-references to external disease ontologies"),
    ExcludeFields("ontology"),
    DocumentField("obsoleteTerms", "Obsoleted ontology terms replaced by this term"),
    DocumentField("directLocationIds", "EFO terms for direct anatomical locations"),
    DocumentField("indirectLocationIds",
                  "EFO terms for indirect anatomical locations (propagated)"
    ),
    DocumentField("ancestors",
                  "Ancestor disease nodes in the EFO ontology up to the top-level therapeutic area"
    ),
    DocumentField("descendants", "Descendant disease nodes in the EFO ontology below this term"),
    ReplaceField(
      "therapeuticAreas",
      Field(
        "therapeuticAreas",
        ListType(diseaseImp),
        Some(
          "Ancestor therapeutic area nodes the disease or phenotype term belongs in the EFO ontology"
        ),
        resolve = r => diseasesFetcher.deferSeq(r.value.therapeuticAreas)
      )
    ),
    ReplaceField(
      "parents",
      Field(
        "parents",
        ListType(diseaseImp),
        Some("Immediate parent disease nodes in the ontology"),
        resolve = r => diseasesFetcher.deferSeq(r.value.parents)
      )
    ),
    ReplaceField(
      "children",
      Field(
        "children",
        ListType(diseaseImp),
        Some("Direct child disease nodes in the ontology"),
        resolve = r => diseasesFetcher.deferSeq(r.value.children)
      )
    ),
    AddFields(
      Field(
        "directLocations",
        ListType(diseaseImp),
        Some("Diseases mapped to direct anatomical locations"),
        resolve = r => diseasesFetcher.deferSeqOpt(r.value.directLocationIds.getOrElse(Seq.empty))
      ),
      Field(
        "indirectLocations",
        ListType(diseaseImp),
        Some("Diseases mapped via indirect (propagated) anatomical locations"),
        resolve = r => diseasesFetcher.deferSeqOpt(r.value.indirectLocationIds.getOrElse(Seq.empty))
      ),
      Field(
        "similarEntities",
        ListType(similarityGQLImp),
        description = Some("Semantically similar diseases based on a PubMed word embedding model"),
        arguments = idsArg :: entityNames :: thresholdArg :: pageSize :: Nil,
        complexity = Some(complexityCalculator(pageSize)),
        resolve = c => {
          val ids = c.arg(idsArg).getOrElse(List.empty)
          val thres = c.arg(thresholdArg).getOrElse(0.1)
          val cats = c.arg(entityNames).getOrElse(Nil).toList
          val n = c.arg(pageSize).getOrElse(10)

          c.ctx.getSimilarW2VEntities(c.value.id, ids.toSet, cats, thres, n)
        }
      ),
      Field(
        "literatureOcurrences",
        publicationsImp,
        description =
          Some("Publications that mention this disease, alone or alongside other entities"),
        arguments = idsArg :: startYear :: startMonth :: endYear :: endMonth :: cursor :: Nil,
        resolve = c => {
          val ids = c.arg(idsArg).getOrElse(List.empty) ++ List(c.value.id)
          val filterStartYear = c.arg(startYear)
          val filterStartMonth = c.arg(startMonth)
          val filterEndYear = c.arg(endYear)
          val filterEndMonth = c.arg(endMonth)
          val cur = c.arg(cursor)

          c.ctx.getLiteratureOcurrences(ids.toSet,
                                        filterStartYear,
                                        filterStartMonth,
                                        filterEndYear,
                                        filterEndMonth,
                                        cur
          )
        }
      ),
      Field(
        "isTherapeuticArea",
        BooleanType,
        description = Some("Whether this disease node is a top-level therapeutic area"),
        resolve = ctx => ctx.value.ontology.isTherapeuticArea
      ),
      Field(
        "phenotypes",
        OptionType(diseaseHPOsImp),
        description = Some(
          "Human Phenotype Ontology (HPO) annotations linked to this disease as clinical signs or symptoms"
        ),
        arguments = pageArg :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = ctx => ctx.ctx.getDiseaseHPOs(ctx.value.id, ctx.arg(pageArg))
      ),
      Field(
        "evidences",
        evidencesImp,
        description =
          Some("Target–disease evidence items supporting associations for this disease"),
        arguments =
          ensemblIds :: indirectEvidences :: datasourceIdsArg :: pageSize :: cursor :: Nil,
        complexity = Some(complexityCalculator(pageSize)),
        resolve = ctx => {
          val indirects = ctx.arg(indirectEvidences).getOrElse(true)
          val efos = if (indirects) ctx.value.id +: ctx.value.descendants else ctx.value.id +: Nil

          ctx.ctx.getEvidencesByEfoId(
            ctx arg datasourceIdsArg,
            ctx arg ensemblIds,
            efos,
            Some(("score", "desc")),
            ctx arg pageSize,
            ctx arg cursor
          )
        }
      ),
      Field(
        "otarProjects",
        ListType(otarProjectImp),
        description = Some(
          "Open Targets (OTAR) projects linked to this disease. Data only available in Partner Platform Preview (PPP)"
        ),
        resolve = r =>
          DeferredValue(otarProjectsFetcher.deferOpt(r.value.id)).map {
            case Some(otars) => otars.rows
            case None        => Seq.empty
          }
      ),
      Field(
        "associatedTargets",
        associatedOTFTargetsImp,
        description = Some(
          "Target–disease associations computed on the fly with configurable datasource weights and filters"
        ),
        arguments =
          BIds :: indirectEvidences :: datasourceSettingsListArg :: facetFiltersListArg :: BFilterString :: scoreSorting :: pageArg :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = ctx =>
          (ctx arg datasourceSettingsListArg) foreach { settingsList =>
            val invalidWeights =
              for setting <- settingsList if setting.weight > 1 || setting.weight < 0
              yield s"The assigned weight for '${setting.id}' was ${setting.weight}"
            if invalidWeights.nonEmpty then
              val errors = Vector(
                invalidWeights.map(invWeight =>
                  InvalidArgValueError("weight", "between 0 and 1", Some(invWeight))
                )*
              )
              throw InputParameterCheckError(errors)
          }
          ctx.ctx.getAssociationsDiseaseFixed(
            ctx.value,
            ctx arg datasourceSettingsListArg,
            ctx arg indirectEvidences getOrElse (true),
            ctx arg facetFiltersListArg getOrElse (Seq.empty),
            ctx arg BIds map (_.toSet) getOrElse (Set.empty),
            ctx arg BFilterString,
            (ctx arg scoreSorting) map (_.split(" ").take(2).toList match {
              case a :: b :: Nil => (a, b)
              case a :: Nil      => (a, "desc")
              case _             => ("score", "desc")
            }),
            ctx arg pageArg
          )
      ),
      Field(
        "resolvedAncestors",
        ListType(diseaseImp),
        description = Some(
          "All ancestor diseases in the ontology from this term up to the top-level therapeutic area"
        ),
        arguments = Nil,
        resolve = ctx => diseasesFetcher.deferSeq(ctx.value.ancestors)
      ),
      Field(
        "drugAndClinicalCandidates",
        clinicalIndicationsFromDiseaseImp,
        description = Some(
          "Clinical indications for this disease as reported by clinical trial records."
        ),
        arguments = Nil,
        resolve = ctx => ctx.ctx.getClinicalIndicationsByDisease(ctx.value.id)
      )
    )
  )

  implicit val tractabilityImp: ObjectType[Backend, Tractability] =
    deriveObjectType[Backend, Tractability](
      ObjectTypeDescription(
        "Tractability information for the target. Indicates the feasibility of targeting the gene/protein with different therapeutic modalities."
      ),
      RenameField("id", "label"),
      DocumentField("id", "Tractability category label"),
      DocumentField("modality", "Modality of the tractability assessment"),
      DocumentField("value", "Tractability value assigned to the target (true indicates tractable)")
    )

  implicit val scoredDataTypeImp: ObjectType[Backend, ScoredComponent] =
    deriveObjectType[Backend, ScoredComponent](
      ObjectTypeDescription("Scored component used in association scoring"),
      DocumentField("id", "Component identifier (e.g., datatype or datasource name)"),
      DocumentField(
        "score",
        "Association score for the component. Scores are normalized to a range of 0-1. The higher the score, the stronger the association."
      )
    )

  implicit val associatedOTFTargetImp: ObjectType[Backend, Association] =
    deriveObjectType[Backend, Association](
      ObjectTypeName("AssociatedTarget"),
      ObjectTypeDescription("Associated target entity"),
      DocumentField(
        "score",
        "Overall association score aggregated across all evidence types. A higher score indicates a stronger association between the target and the disease. Scores are normalized to a range of 0-1."
      ),
      DocumentField(
        "datatypeScores",
        "Association scores computed for every datatype (e.g., Genetic associations, Somatic, Literature)"
      ),
      DocumentField(
        "datasourceScores",
        "Association scores computed for every datasource (e.g., IMPC, ChEMBL, Gene2Phenotype)"
      ),
      ReplaceField(
        "id",
        Field("target",
              targetImp,
              Some("Associated target entity"),
              resolve = r => targetsFetcher.defer(r.value.id)
        )
      )
    )

  implicit val associatedOTFDiseaseImp: ObjectType[Backend, Association] =
    deriveObjectType[Backend, Association](
      ObjectTypeName("AssociatedDisease"),
      ObjectTypeDescription("Associated disease entity"),
      DocumentField(
        "score",
        "Overall association score aggregated across all evidence types. A higher score indicates a stronger association between the target and the disease. Scores are normalized to a range of 0-1."
      ),
      DocumentField(
        "datatypeScores",
        "Association scores computed for every datatype (e.g., Genetic associations, Somatic, Literature)"
      ),
      DocumentField(
        "datasourceScores",
        "Association scores computed for every datasource (e.g., IMPC, ChEMBL, Gene2Phenotype)"
      ),
      ReplaceField(
        "id",
        Field(
          "disease",
          diseaseImp,
          Some("Associated disease entity"),
          resolve = r => diseasesFetcher.defer(r.value.id)
        )
      )
    )

  implicit lazy val reactomeImp: ObjectType[Backend, Reactome] =
    deriveObjectType[Backend, Reactome](
      AddFields(
        Field(
          "isRoot",
          BooleanType,
          description = Some("If the node is root"),
          resolve = _.value.isRoot
        )
      ),
      ReplaceField(
        "children",
        Field(
          "children",
          ListType(reactomeImp),
          Some("Direct child pathway nodes that descend from this pathway"),
          resolve = r => reactomeFetcher.deferSeqOpt(r.value.children)
        )
      ),
      ReplaceField(
        "parents",
        Field(
          "parents",
          ListType(reactomeImp),
          Some("Immediate parent pathway nodes of this pathway"),
          resolve = r => reactomeFetcher.deferSeqOpt(r.value.parents)
        )
      ),
      ReplaceField(
        "ancestors",
        Field(
          "ancestors",
          ListType(reactomeImp),
          Some("All ancestor pathway nodes up to the root of the hierarchy"),
          resolve = r => reactomeFetcher.deferSeqOpt(r.value.ancestors)
        )
      )
    )

  implicit val tissueImp: ObjectType[Backend, Tissue] = deriveObjectType[Backend, Tissue](
    ObjectTypeDescription(
      "Baseline RNA and protein expression data across tissues. This data does not contain raw expression values, instead to shows how targets are selectively expressed across different tissues. This dataset combines expression values from multiple sources including Expression Atlas and Human Protein Atlas."
    ),
    DocumentField("id", "UBERON id"),
    DocumentField("label", "Name of the biosample the expression data is from"),
    DocumentField("anatomicalSystems",
                  "List of anatomical systems that the biosample can be found in"
    ),
    DocumentField("organs", "List of organs that the biosample can be found in")
  )
  implicit val rnaExpressionImp: ObjectType[Backend, RNAExpression] =
    deriveObjectType[Backend, RNAExpression](
      ObjectTypeDescription(
        "RNA expression values for a particular biosample and gene combination"
      ),
      DocumentField("zscore", "Expression zscore"),
      DocumentField("value", "Expression value"),
      DocumentField("unit", "Unit for the RNA expression"),
      DocumentField("level", "Level of RNA expression normalised to 0-5 or -1 if absent")
    )
  implicit val cellTypeImp: ObjectType[Backend, CellType] = deriveObjectType[Backend, CellType](
    ObjectTypeDescription("Cell type where protein levels were measured"),
    DocumentField("reliability", "Reliability of the cell type measurement"),
    DocumentField("name", "Cell type name"),
    DocumentField("level", "Level of expression for this cell type")
  )
  implicit val proteinExpressionImp: ObjectType[Backend, ProteinExpression] =
    deriveObjectType[Backend, ProteinExpression](
      ObjectTypeDescription(
        "Struct containing relevant protein expression values for a particular biosample and gene combination"
      ),
      DocumentField("reliability", "Reliability of the protein expression measurement"),
      DocumentField("level", "Level of protein expression normalised to 0-5 or -1 if absent"),
      DocumentField("cellType", "List of cell types were protein levels were measured")
    )
  implicit val expressionImp: ObjectType[Backend, Expression] =
    deriveObjectType[Backend, Expression](
      ObjectTypeDescription(
        "Array of structs containing expression data relevant to a particular gene and biosample combination"
      ),
      DocumentField("tissue", "Tissue/biosample information for the expression data"),
      DocumentField("rna", "RNA expression values for the biosample and gene combination"),
      DocumentField("protein", "Protein expression values for the biosample and gene combination")
    )
  implicit val expressionsImp: ObjectType[Backend, Expressions] =
    deriveObjectType[Backend, Expressions](
      ObjectTypeDescription(
        "Baseline RNA and protein expression data across tissues for a target gene"
      ),
      ExcludeFields("id"),
      DocumentField("rows",
                    "Array of structs containing expression data relevant to a particular gene"
      )
    )

  implicit val adverseEventImp: ObjectType[Backend, AdverseEvent] =
    deriveObjectType[Backend, AdverseEvent](
      ObjectTypeDescription(
        "Significant adverse events associated with drugs sharing the same pharmacological target. This dataset is based on the FDA's Adverse Event Reporting System (FAERS) reporting post-marketing surveillance data and it's filtered to include only reports submitted by health professionals. The significance of a given target-ADR is estimated using a Likelihood Ratio Test (LRT) using all reports associated with the drugs with the same target."
      ),
      DocumentField("name", "Meddra term on adverse event"),
      DocumentField("meddraCode", "8 digit unique meddra identification number"),
      DocumentField("count", "Number of reports mentioning drug and adverse event"),
      DocumentField("logLR", "Log-likelihood ratio"),
      ExcludeFields("criticalValue")
    )

  implicit val adverseEventsImp: ObjectType[Backend, AdverseEvents] =
    deriveObjectType[Backend, AdverseEvents](
      ObjectTypeDescription(
        "Significant adverse events associated with drugs sharing the same pharmacological target. This dataset is based on the FDA's Adverse Event Reporting System (FAERS) reporting post-marketing surveillance data and it's filtered to include only reports submitted by health professionals. The significance of a given target-ADR is estimated using a Likelihood Ratio Test (LRT) using all reports associated with the drugs with the same target."
      ),
      DocumentField("count", "Total significant adverse events"),
      DocumentField("criticalValue", "LLR critical value to define significance"),
      DocumentField("rows", "Significant adverse event entries")
    )

  implicit val otarProjectImp: ObjectType[Backend, OtarProject] =
    deriveObjectType[Backend, OtarProject](
      ObjectTypeDescription(
        "Open Targets (OTAR) project information associated with a disease. Data only available in Partner Platform Preview (PPP)"
      ),
      DocumentField("otarCode", "OTAR project code identifier"),
      DocumentField("status", "Status of the OTAR project"),
      DocumentField("projectName", "Name of the OTAR project"),
      DocumentField("reference", "Reference or citation for the OTAR project"),
      DocumentField("integratesInPPP",
                    "Whether the project integrates data in the Open Targets Partner Preview (PPP)"
      )
    )
  implicit val otarProjectsImp: ObjectType[Backend, OtarProjects] =
    deriveObjectType[Backend, OtarProjects](
      ObjectTypeDescription("Collection of Open Targets (OTAR) projects associated with a disease"),
      DocumentField("efoId", "EFO disease identifier"),
      DocumentField("rows", "List of OTAR projects associated with the disease")
    )

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit val geneOntologyImp: ObjectType[Backend, GeneOntology] =
    deriveObjectType[Backend, GeneOntology](
      ObjectTypeDescription("Gene Ontology (GO) annotations related to the target"),
      ReplaceField(
        "id",
        Field(
          "term",
          geneOntologyTermImp,
          description = Some("Gene ontology term"),
          resolve = r =>
            DeferredValue(goFetcher.deferOpt(r.value.id)).map {
              case Some(value) => value
              case None =>
                logger.warn(s"go was not found in go index using default go name",
                            keyValue("id", r.value.id)
                )
                GeneOntologyTerm(r.value.id, "Name unknown in Open Targets")
            }
        )
      ),
      DocumentField(
        "aspect",
        "Type of the GO annotation: molecular function (F), biological process (P) and cellular localisation (C)"
      ),
      DocumentField("evidence", "Evidence supporting the GO annotation"),
      DocumentField("geneProduct",
                    "Gene product associated with the GO annotation [bioregistry:uniprot]"
      ),
      DocumentField("source",
                    "Source database and identifier where the ontology term was sourced from"
      )
    )

  implicit val cancerHallmarkImp: ObjectType[Backend, CancerHallmark] =
    deriveObjectType[Backend, CancerHallmark](
      ObjectTypeDescription("Cancer hallmarks associated with the target gene"),
      DocumentField("description", "Description of the cancer hallmark"),
      DocumentField("impact", "Impact of the cancer hallmark on the target"),
      DocumentField("label", "Label associated with the cancer hallmark"),
      DocumentField(
        "pmid",
        "PubMed ID of the supporting literature for the cancer hallmark [bioregistry:pubmed]"
      )
    )
  implicit val hallmarksAttributeImp: ObjectType[Backend, HallmarkAttribute] =
    deriveObjectType[Backend, HallmarkAttribute](
      ObjectTypeDescription("Attributes of the hallmark annotation"),
      RenameField("attribute_name", "name"),
      DocumentField("attribute_name", "Name of the hallmark attribute"),
      DocumentField("description", "Description of the hallmark attribute"),
      DocumentField(
        "pmid",
        "PubMed ID of the supporting literature for the hallmark attribute [bioregistry:pubmed]"
      )
    )
  implicit val hallmarksImp: ObjectType[Backend, Hallmarks] = deriveObjectType[Backend, Hallmarks](
    ObjectTypeDescription("Hallmarks related to the target gene sourced from COSMIC"),
    DocumentField("cancerHallmarks", "Cancer hallmarks associated with the target gene"),
    DocumentField("attributes", "Attributes of the hallmark annotation")
  )

  implicit val mousePhenotypeBiologicalModel: ObjectType[Backend, BiologicalModels] =
    deriveObjectType[Backend, BiologicalModels](
      ObjectTypeDescription("Container for all biological model-related attributes"),
      DocumentField("allelicComposition", "The specific allelic composition of the mouse model"),
      DocumentField("geneticBackground", "The genetic background strain of the mouse model"),
      DocumentField("id", "Unique identifier for the biological model [bioregistry:mgi]"),
      DocumentField("literature", "References related to the mouse model [bioregistry:pubmed]")
    )
  implicit val mousePhenotypeModelPhenotypeClasses: ObjectType[Backend, ModelPhenotypeClasses] =
    deriveObjectType[Backend, ModelPhenotypeClasses](
      ObjectTypeDescription("Container for phenotype class-related attributes"),
      DocumentField("id", "Unique identifier for the phenotype class [bioregistry:mp]"),
      DocumentField("label", "Descriptive label for the phenotype class")
    )

  implicit val mousePhenotypeImp: ObjectType[Backend, MousePhenotype] =
    deriveObjectType[Backend, MousePhenotype](
      ObjectTypeDescription(
        "Mouse phenotype information linking human targets to observed phenotypes in mouse models"
      ),
      ExcludeFields("targetFromSourceId"),
      DocumentField("biologicalModels", "Container for all biological model-related attributes"),
      DocumentField("modelPhenotypeClasses", "Container for phenotype class-related attributes"),
      DocumentField("modelPhenotypeId",
                    "Identifier for the specific phenotype observed in the model [bioregistry:mp]"
      ),
      DocumentField("modelPhenotypeLabel",
                    "Human-readable label describing the observed phenotype"
      ),
      DocumentField("targetInModel", "Name of the target gene as represented in the mouse model"),
      DocumentField("targetInModelEnsemblId",
                    "Ensembl identifier for the target gene in the mouse model"
      ),
      DocumentField("targetInModelMgiId",
                    "MGI identifier for the target gene in the mouse model [bioregistry:mgi]"
      )
    )

  implicit val tepImp: ObjectType[Backend, Tep] = deriveObjectType[Backend, Tep](
    ObjectTypeDescription("Target Enabling Package (TEP) information"),
    DocumentField("targetFromSourceId", "Ensembl gene ID for the TEP target"),
    DocumentField("description", "Description of the TEP target"),
    DocumentField("therapeuticArea", "Therapeutic area associated with the TEP target"),
    DocumentField("url", "URL linking to more information on the TEP target"),
    RenameField("targetFromSourceId", "name"),
    RenameField("url", "uri")
  )

  implicit val idAndSourceImp: ObjectType[Backend, IdAndSource] =
    deriveObjectType[Backend, IdAndSource](
      ObjectTypeDescription("Identifier with source information"),
      DocumentField("id", "Identifier value"),
      DocumentField("source", "Source database or organization providing the identifier")
    )
  implicit val locationAndSourceImp: ObjectType[Backend, LocationAndSource] =
    deriveObjectType[Backend, LocationAndSource](
      ObjectTypeDescription("Subcellular location information with source"),
      DocumentField("location", "Name of the subcellular compartment where the protein was found"),
      DocumentField("source", "Source database for the subcellular location"),
      DocumentField("termSL",
                    "Subcellular location term identifier from SwissProt [bioregistry:sl]"
      ),
      DocumentField("labelSL", "Subcellular location category from SwissProt")
    )
  implicit val labelAndSourceImp: ObjectType[Backend, LabelAndSource] =
    deriveObjectType[Backend, LabelAndSource](
      ObjectTypeDescription("Label with source information"),
      DocumentField("label", "Label value (e.g., synonym, symbol)"),
      DocumentField("source", "Source database of the label")
    )
  implicit val genomicLocationImp: ObjectType[Backend, GenomicLocation] =
    deriveObjectType[Backend, GenomicLocation](
      ObjectTypeDescription("Genomic location information of the target gene"),
      DocumentField("chromosome", "Chromosome on which the target is located"),
      DocumentField("start", "Genomic start position of the target gene"),
      DocumentField("end", "Genomic end position of the target gene"),
      DocumentField("strand", "Strand orientation of the target gene")
    )
  implicit val targetClassImp: ObjectType[Backend, TargetClass] =
    deriveObjectType[Backend, TargetClass](
      ObjectTypeDescription("Target classification categories from ChEMBL"),
      DocumentField("id", "Unique identifier for the target class"),
      DocumentField("label", "Label for the target class"),
      DocumentField("level", "Hierarchical level of the target class")
    )
  implicit val doseTypeImp: ObjectType[Backend, SafetyEffects] =
    deriveObjectType[Backend, SafetyEffects](
      ObjectTypeDescription("Effects reported for safety events"),
      DocumentField("direction", "Direction of the reported effect (e.g., increase or decrease)"),
      DocumentField("dosing", "Dosing conditions related to the reported effect")
    )
  implicit val canonicalTranscriptImp: ObjectType[Backend, CanonicalTranscript] =
    deriveObjectType[Backend, CanonicalTranscript](
      ObjectTypeDescription("The Ensembl canonical transcript of the target gene"),
      DocumentField("id", "The Ensembl transcript identifier for the canonical transcript"),
      DocumentField("chromosome", "Chromosome location of the canonical transcript"),
      DocumentField("start", "Genomic start position of the canonical transcript"),
      DocumentField("end", "Genomic end position of the canonical transcript"),
      DocumentField("strand", "Strand orientation of the canonical transcript")
    )
  implicit val constraintImp: ObjectType[Backend, Constraint] =
    deriveObjectType[Backend, Constraint](
      ObjectTypeDescription(
        "Constraint scores for the target gene from GnomAD. Indicates gene intolerance to loss-of-function mutations."
      ),
      DocumentField("constraintType", "Type of constraint applied to the target"),
      DocumentField("score", "Constraint score indicating gene intolerance"),
      DocumentField("exp", "Expected constraint score"),
      DocumentField("obs", "Observed constraint score"),
      DocumentField("oe", "Observed/Expected (OE) constraint score"),
      DocumentField("oeLower", "Lower bound of the OE constraint score"),
      DocumentField("oeUpper", "Upper bound of the OE constraint score"),
      DocumentField("upperBin",
                    "Upper bin classification going from more constrained to less constrained"
      ),
      DocumentField(
        "upperBin6",
        "Interpretable classification of constraint based on 6 bins. [GnomAD labels: 0: `very high`, 1: `high`, 2: `medium`, 3: `low`, 4: `very low`, 5: `very low`]"
      ),
      DocumentField(
        "upperRank",
        "Upper rank classification for every coding gene assessed by GnomAD going from more constrained to less constrained"
      )
    )
  implicit val homologueImp: ObjectType[Backend, Homologue] = deriveObjectType[Backend, Homologue](
    ObjectTypeDescription(
      "Homologues of the target gene in other species according to Ensembl Compara"
    ),
    DocumentField("homologyType", "Type of homology relationship"),
    DocumentField("queryPercentageIdentity",
                  "Percentage identity of the query gene in the homologue"
    ),
    DocumentField("speciesId", "Species ID for the homologue"),
    DocumentField("speciesName", "Species name for the homologue"),
    DocumentField("targetGeneId", "Gene ID of the homologue"),
    DocumentField("targetGeneSymbol", "Gene symbol of the homologous target"),
    DocumentField("targetPercentageIdentity",
                  "Percentage identity of the homologue in the query gene"
    ),
    DocumentField("isHighConfidence",
                  "Indicates if the homology is high confidence according to Ensembl Compara"
    )
  )
  implicit val targetBiosampleImp: ObjectType[Backend, SafetyBiosample] =
    deriveObjectType[Backend, SafetyBiosample](
      ObjectTypeDescription("Biosamples used in safety assessments"),
      DocumentField("tissueLabel", "Label of the biosample tissue"),
      DocumentField("tissueId", "Tissue ID for the biosample"),
      DocumentField("cellLabel", "Label of the biosample cell"),
      DocumentField("cellFormat", "Format of the biosample cells"),
      DocumentField("cellId", "Cell identifier for the biosample")
    )
  implicit val targetSafetyStudyImp: ObjectType[Backend, SafetyStudy] =
    deriveObjectType[Backend, SafetyStudy](
      ObjectTypeDescription("Studies related to safety assessments"),
      DocumentField("name", "Name of the safety study"),
      DocumentField("description", "Description of the safety study"),
      DocumentField("type", "Type of safety study")
    )
  implicit val safetyLiabilityImp: ObjectType[Backend, SafetyLiability] =
    deriveObjectType[Backend, SafetyLiability](
      ObjectTypeDescription("Safety liabilities associated with the target"),
      DocumentField("biosamples", "Biosamples used in safety assessments"),
      DocumentField("datasource", "Data source reporting the safety liability"),
      DocumentField("effects", "Effects reported for the safety event"),
      DocumentField("event", "Safety event associated with the target"),
      DocumentField("eventId", "Unique identifier for the safety event"),
      DocumentField("literature", "Literature references for the safety liability"),
      DocumentField("url", "URL linking to more details on safety liabilities"),
      DocumentField("studies", "Studies related to safety assessments")
    )

  // hpo
  implicit lazy val hpoImp: ObjectType[Backend, HPO] = deriveObjectType(
    ObjectTypeDescription(
      "Human Phenotype Ontology subset of information included in the Platform."
    ),
    DocumentField("id", "Open Targets hpo id"),
    DocumentField("name", "Phenotype name"),
    DocumentField("description", "Phenotype description"),
    DocumentField("namespace", "namespace")
  )

  // DiseaseHPO
  // More details here: https://hpo.jax.org/app/help/annotations
  implicit val diseaseHPOEvidencesImp: ObjectType[Backend, DiseaseHPOEvidences] =
    deriveObjectType[Backend, DiseaseHPOEvidences](
      ObjectTypeDescription(
        "the HPO project provides a large set of phenotype annotations. Source: Phenotype.hpoa"
      ),
      DocumentField(
        "aspect",
        "One of P (Phenotypic abnormality), I (inheritance), C (onset and clinical course). Might be null (MONDO)"
      ),
      DocumentField(
        "bioCuration",
        "This refers to the center or user making the annotation and the date on which the annotation was made"
      ),
      DocumentField(
        "diseaseFromSourceId",
        "This field refers to the database and database identifier. EG. OMIM"
      ),
      DocumentField("diseaseFromSource", "Related name from the field diseaseFromSourceId"),
      ExcludeFields("diseaseName"),
      DocumentField(
        "evidenceType",
        "This field indicates the level of evidence supporting the annotation."
      ),
      DocumentField("frequency", "A term-id from the HPO-sub-ontology"),
      ReplaceField(
        "modifiers",
        Field(
          "modifiers",
          ListType(hpoImp),
          Some("HP terms from the Clinical modifier subontology"),
          resolve = r => hposFetcher.deferSeqOpt(r.value.modifiers)
        )
      ),
      ReplaceField(
        "onset",
        Field(
          "onset",
          ListType(hpoImp),
          Some("A term-id from the HPO-sub-ontology below the term Age of onset."),
          resolve = r => hposFetcher.deferSeqOpt(r.value.onset)
        )
      ),
      DocumentField(
        "qualifierNot",
        "This optional field can be used to qualify the annotation. Values: [True or False]"
      ),
      DocumentField(
        "references",
        "This field indicates the source of the information used for the annotation (phenotype.hpoa)"
      ),
      DocumentField(
        "sex",
        "This field contains the strings MALE or FEMALE if the annotation in question is limited to males or females."
      ),
      DocumentField("resource", "Possible source mapping: HPO or MONDO"),
      AddFields(
        Field(
          "frequencyHPO",
          OptionType(hpoImp),
          Some("HPO Entity"),
          resolve = r => hposFetcher.deferOpt(r.value.frequency)
        )
      )
    )
  implicit val diseaseHPOImp: ObjectType[Backend, DiseaseHPO] =
    deriveObjectType[Backend, DiseaseHPO](
      ObjectTypeDescription("Disease and phenotypes annotations"),
      ReplaceField(
        "phenotype",
        Field(
          "phenotypeHPO",
          OptionType(hpoImp),
          Some("Phenotype entity"),
          resolve = r => hposFetcher.deferOpt(r.value.phenotype)
        )
      ),
      AddFields(
        Field(
          "phenotypeEFO",
          OptionType(diseaseImp),
          Some("Disease Entity"),
          resolve = r => diseasesFetcher.deferOpt(r.value.phenotype)
        )
      ),
      ExcludeFields("disease"),
      DocumentField("evidence", "List of phenotype annotations.")
    )

  implicit val diseaseHPOsImp: ObjectType[Backend, DiseaseHPOs] =
    deriveObjectType[Backend, DiseaseHPOs](
      ObjectTypeDescription(
        "Human Phenotype Ontology (HPO) annotations associated with the disease"
      ),
      DocumentField("count", "Total number of phenotype annotations"),
      DocumentField("rows", "List of phenotype annotations for the disease")
    )

  implicit lazy val drugReferenceImp: ObjectType[Backend, Reference] =
    deriveObjectType[Backend, Reference](
      ObjectTypeDescription("Reference information supporting the drug mechanisms of action"),
      DocumentField("ids", "List of reference identifiers"),
      DocumentField("source", "Source of the reference (e.g., PubMed, FDA, package inserts)"),
      DocumentField("urls", "List of URLs linking to the reference")
    )

  implicit lazy val drugWithIdsImp: ObjectType[Backend, DrugWithIdentifiers] =
    deriveObjectType[Backend, DrugWithIdentifiers](
      ObjectTypeName("DrugWithIdentifiers"),
      ObjectTypeDescription("Drug with drug identifiers"),
      DocumentField("drugId", "Drug or clinical candidate identifier"),
      DocumentField("drugFromSource", "Drug identifier from the original data source"),
      AddFields(
        Field(
          "drug",
          OptionType(drugImp),
          description = Some("Drug or clinical candidate entity"),
          resolve = r => drugsFetcher.deferOpt(r.value.drugId)
        )
      )
    )

  implicit val sequenceOntologyTermImp: ObjectType[Backend, SequenceOntologyTerm] =
    deriveObjectType[Backend, SequenceOntologyTerm](
      ObjectTypeName("SequenceOntologyTerm"),
      ObjectTypeDescription("Sequence ontology term identifier and name"),
      DocumentField("id", "Sequence ontology term identifier [bioregistry:so]"),
      DocumentField("label", "Sequence ontology term label (e.g. 'missense_variant')")
    )

  implicit lazy val variantAnnotationImp: ObjectType[Backend, VariantAnnotation] =
    deriveObjectType[Backend, VariantAnnotation](
      ObjectTypeDescription(
        "Genetic variants influencing individual drug responses. Pharmacogenetics data is integrated from sources including Pharmacogenomics Knowledgebase (PharmGKB)."
      ),
      DocumentField("literature", "PubMed identifier (PMID) of the literature entry"),
      DocumentField("effectDescription",
                    "Summary of the impact of the allele on the drug response."
      ),
      DocumentField("effectType", "Type of effect."),
      DocumentField("baseAlleleOrGenotype", "Allele or genotype in the base case."),
      DocumentField("comparisonAlleleOrGenotype", "Allele or genotype in the comparison case."),
      DocumentField(
        "directionality",
        "Indicates in which direction the genetic variant increases or decreases drug response"
      ),
      DocumentField("effect", "Allele observed effect."),
      DocumentField("entity", "Entity affected by the effect.")
    )

  implicit lazy val pharmacogenomicsImp: ObjectType[Backend, Pharmacogenomics] =
    deriveObjectType[Backend, Pharmacogenomics](
      ObjectTypeDescription(
        "Pharmacogenomics data linking genetic variants to drug responses. Data is integrated from sources including ClinPGx."
      ),
      DocumentField("datasourceId", "Identifier for the data provider"),
      DocumentField("datatypeId",
                    "Classification of the type of pharmacogenomic data (e.g., clinical_annotation)"
      ),
      DocumentField("evidenceLevel",
                    "Strength of the scientific support for the variant/drug response"
      ),
      DocumentField("genotype", "Genetic variant configuration"),
      DocumentField("genotypeAnnotationText",
                    "Explanation of the genotype's clinical significance"
      ),
      DocumentField("genotypeId",
                    "Identifier for the specific genetic variant combination (e.g., 1_1500_A_A,T)"
      ),
      DocumentField("haplotypeFromSourceId", "Haplotype ID in the ClinPGx dataset"),
      DocumentField(
        "haplotypeId",
        "Combination of genetic variants that constitute a particular allele of a gene (e.g., CYP2C9*3)"
      ),
      DocumentField("literature",
                    "PubMed identifier (PMID) of the literature entry [bioregistry:pubmed]"
      ),
      DocumentField("pgxCategory", "Classification of the drug response type (e.g., Toxicity)"),
      DocumentField("phenotypeFromSourceId", "Phenotype identifier from the source"),
      DocumentField("phenotypeText", "Description of the phenotype associated with the variant"),
      DocumentField("variantAnnotation",
                    "Annotation details about the variant effect on drug response"
      ),
      DocumentField("studyId", "Identifier of the study providing the pharmacogenomic evidence"),
      DocumentField("variantRsId", "dbSNP rsID identifier for the variant"),
      DocumentField("variantId", "Variant identifier in CHROM_POS_REF_ALT notation"),
      DocumentField(
        "variantFunctionalConsequenceId",
        "The sequence ontology identifier of the consequence of the variant based on Ensembl VEP in the context of the transcript [bioregistry:so]"
      ),
      DocumentField("targetFromSourceId",
                    "Target (gene/protein) identifier as reported by the data source"
      ),
      DocumentField("isDirectTarget", "Whether the target is directly affected by the variant"),
      AddFields(
        Field(
          "variantFunctionalConsequence",
          OptionType(sequenceOntologyTermImp),
          description = Some(
            "The sequence ontology identifier of the consequence of the variant based on Ensembl VEP in the context of the transcript [bioregistry:so]"
          ),
          resolve = r => {
            val soId = (r.value.variantFunctionalConsequenceId)
              .map(id => id.replace("_", ":"))
            logger.debug(s"finding variant functional consequence", keyValue("id", soId))
            soTermsFetcher.deferOpt(soId)
          }
        ),
        Field(
          "target",
          OptionType(targetImp),
          description = Some("Target entity"),
          resolve = r => targetsFetcher.deferOpt(r.value.targetFromSourceId)
        )
      ),
      ReplaceField(
        "drugs",
        Field(
          "drugs",
          ListType(drugWithIdsImp),
          description =
            Some("List of drugs or clinical candidates associated with the pharmacogenomic data"),
          resolve = r => r.value.drugs
        )
      )
    )

  implicit lazy val indicationReferenceImp: ObjectType[Backend, IndicationReference] =
    deriveObjectType[Backend, IndicationReference](
      ObjectTypeDescription("Reference information for drug indications"),
      DocumentField("ids", "List of reference identifiers (e.g., PubMed IDs)"),
      DocumentField("source", "Source of the reference")
    )

  implicit lazy val mechanismOfActionRowImp: ObjectType[Backend, MechanismOfActionRow] =
    deriveObjectType[Backend, MechanismOfActionRow](
      ObjectTypeDescription("Mechanism of action information for a drug"),
      DocumentField("mechanismOfAction", "Description of the mechanism of action"),
      DocumentField(
        "actionType",
        "Classification of how the drug interacts with its target (e.g., ACTIVATOR, INHIBITOR)"
      ),
      DocumentField("targetName", "Name of the target molecule"),
      DocumentField("references", "Reference information supporting the mechanism of action"),
      ReplaceField(
        "targets",
        Field(
          "targets",
          ListType(targetImp),
          description = Some(
            "List of on-target (genes or proteins) involved in the drug or clinical candidate mechanism of action"
          ),
          resolve = r => targetsFetcher.deferSeqOpt(r.value.targets.getOrElse(Seq.empty))
        )
      )
    )

  implicit lazy val indicationRowImp: ObjectType[Backend, IndicationRow] =
    deriveObjectType[Backend, IndicationRow](
      ObjectTypeDescription(
        "Indication information linking a drug or clinical candidate molecule to a disease"
      ),
      DocumentField(
        "maxPhaseForIndication",
        "Maximum clinical trial phase for this drug-disease indication. [Values: -1: `Unknown`, 0: `Phase 0`, 0.5: `Phase I (Early)`, 1: `Phase I`, 2: `Phase II`, 3: `Phase III`, 4: `Phase IV`]"
      ),
      DocumentField("references", "Reference information supporting the indication"),
      ReplaceField(
        "disease",
        Field(
          "disease",
          diseaseImp,
          description = Some("Potential indication disease entity"),
          resolve = r => diseasesFetcher.defer(r.value.disease)
        )
      )
    )

  implicit lazy val indicationsImp: ObjectType[Backend, Indications] =
    deriveObjectType[Backend, Indications](
      ObjectTypeDescription("Collection of indications for a drug or clinical candidate molecule"),
      ExcludeFields("id"),
      RenameField("indications", "rows"),
      RenameField("indicationCount", "count"),
      DocumentField("indications", "List of potential indication entries"),
      DocumentField("indicationCount", "Total number of potential indications"),
      DocumentField("approvedIndications", "List of approved indication identifiers")
    )

  implicit lazy val resourceScoreImp: ObjectType[Backend, ResourceScore] =
    deriveObjectType[Backend, ResourceScore](
      ObjectTypeDescription("Score from a specific datasource"),
      DocumentField("name", "Name of the resource providing the score"),
      DocumentField("value", "Score value from the resource")
    )
  implicit lazy val intervalImp: ObjectType[Backend, Interval] =
    deriveObjectType[Backend, Interval](
      ObjectTypeDescription(
        "Regulatory enhancer/promoter regions to gene (target) predictions for a specific tissue/cell type based on the integration of experimental sources"
      ),
      DocumentField("chromosome", "Chromosome containing the regulatory region"),
      DocumentField("start", "Genomic start position of the regulatory region"),
      DocumentField("end", "Genomic end position of the regulatory region"),
      DocumentField("intervalType", "Type of regulatory region (e.g., enhancer, promoter)"),
      DocumentField("distanceToTss",
                    "Distance from the regulatory region to the transcription start site"
      ),
      DocumentField("score", "Combined score for the enhancer/promoter region to gene prediction"),
      DocumentField("resourceScore", "Scores from individual resources used in prediction"),
      DocumentField(
        "datasourceId",
        "Identifier of the data source providing the regulatory region to gene prediction"
      ),
      DocumentField("pmid",
                    "PubMed identifier for the study providing the evidence [bioregistry:pubmed]"
      ),
      DocumentField("studyId", "Identifier of the study providing the experimental data"),
      DocumentField("biosampleName", "Name of the biosample where the interval was identified"),
      ReplaceField(
        "geneId",
        Field("target",
              targetImp,
              description = Some("Predicted gene (target)"),
              resolve = r => targetsFetcher.defer(r.value.geneId)
        )
      ),
      ReplaceField(
        "biosampleId",
        Field(
          "biosample",
          OptionType(biosampleImp),
          description = Some(
            "Cell type or tissue where the regulatory region to gene prediction was identified"
          ),
          resolve = r => biosamplesFetcher.deferOpt(r.value.biosampleId)
        )
      )
    )
  implicit lazy val intervalsImp: ObjectType[Backend, Intervals] =
    deriveObjectType[Backend, Intervals](
      ObjectTypeDescription(
        "Collection of regulatory enhancer/promoter regions to gene (target) predictions for a specific tissue/cell type based on the integration of experimental sources"
      ),
      DocumentField("count", "Total number of enhancer/promoter region to gene predictions"),
      DocumentField("rows", "List of enhancer/promoter region to gene predictions")
    )

  implicit lazy val mechanismOfActionImp: ObjectType[Backend, MechanismsOfAction] =
    deriveObjectType[Backend, MechanismsOfAction](
      ObjectTypeDescription("Collection of mechanisms of action for a drug molecule"),
      DocumentField("rows", "List of mechanism of action entries"),
      DocumentField("uniqueActionTypes", "Unique list of action types across all mechanisms"),
      DocumentField("uniqueTargetTypes", "Unique list of target types across all mechanisms")
    )

  implicit lazy val drugCrossReferenceImp: ObjectType[Backend, DrugReferences] =
    deriveObjectType[Backend, DrugReferences](
      ObjectTypeDescription("Cross-reference information for a drug molecule"),
      DocumentField("source", "Source database providing the cross-reference"),
      DocumentField("ids", "List of identifiers from the source database")
    )
  implicit lazy val drugWarningReferenceImp: ObjectType[Backend, DrugWarningReference] =
    deriveObjectType[Backend, DrugWarningReference](
      ObjectTypeDescription("Reference information for drug warnings"),
      DocumentField("id", "Reference identifier (e.g., PubMed ID)"),
      DocumentField("source", "Source of the reference"),
      DocumentField("url", "URL linking to the reference")
    )

  implicit lazy val drugWarningsImp: ObjectType[Backend, DrugWarning] =
    deriveObjectType[Backend, DrugWarning](
      ObjectTypeDescription(
        "Blackbox and withdrawn information for drugs molecules included in ChEMBL database."
      ),
      DocumentField("id", "Internal identifier for the drug warning record"),
      DocumentField("chemblIds", "List of molecule identifiers associated with the warning"),
      DocumentField("toxicityClass", "Classification of toxicity type associated with the drug"),
      DocumentField("country", "Country where the warning was issued"),
      DocumentField("description", "Description of the drug adverse effect"),
      DocumentField("references", "List of sources supporting the warning information"),
      DocumentField("warningType",
                    "Classification of action taken (drug is withdrawn or has a black box warning)"
      ),
      DocumentField("efoTerm", "List of disease labels associated with the warning"),
      DocumentField("efoId",
                    "List of disease identifiers associated with the warning [bioregistry:efo]"
      ),
      DocumentField("efoIdForWarningClass",
                    "Disease identifier categorising the type of warning [bioregistry:efo]"
      ),
      DocumentField("year", "Year when the warning was issued")
    )

  implicit lazy val drugImp: ObjectType[Backend, Drug] = deriveObjectType[Backend, Drug](
    ObjectTypeDescription(
      "Core annotation for drug or clinical candidate molecules. A drug in the platform is understood as any bioactive molecule with drug-like properties included in the EMBL-EBI ChEMBL database. All ChEMBL molecules fullfilling any of the next criteria are included in the database: a) Molecules with a known indication. b) Molecules with a known mechanism of action c) ChEMBL molecules included in the DrugBank database d) Molecules that are acknowledged as chemical probes"
    ),
    DocumentField("id", "Drug or clinical candidate molecule identifier"),
    DocumentField("name", "Generic name of the drug molecule"),
    DocumentField("synonyms", "List of alternative names for the drug"),
    DocumentField("tradeNames", "List of brand names for the drug"),
    DocumentField(
      "drugType",
      "Classification of the molecule's therapeutic category or chemical class (e.g. Antibody)"
    ),
    DocumentField(
      "maximumClinicalTrialPhase",
      "Highest clinical trial phase reached by the drug or clinical candidate molecule. [Values: -1: `Unknown`, 0: `Phase 0`, 0.5: `Phase I (Early)`, 1: `Phase I`, 2: `Phase II`, 3: `Phase III`, 4: `Phase IV`]"
    ),
    DocumentField("isApproved",
                  "Flag indicating whether the drug has received regulatory approval"
    ),
    DocumentField("hasBeenWithdrawn", "Flag indicating whether the drug was removed from market"),
    DocumentField("blackBoxWarning", "Flag indicating whether the drug has safety warnings"),
    DocumentField("crossReferences",
                  "Cross-reference information for this molecule from external databases"
    ),
    DocumentField("description", "Summary of the drug's clinical development"),
    ReplaceField(
      "parentId",
      Field(
        "parentMolecule",
        OptionType(drugImp),
        description = Some("Parent molecule for derivative compounds"),
        resolve = r => drugsFetcher.deferOpt(r.value.parentId)
      )
    ),
    ReplaceField(
      "childChemblIds",
      Field(
        "childMolecules",
        ListType(drugImp),
        description = Some("List of molecules corresponding to derivative compounds"),
        resolve = r => drugsFetcher.deferSeqOpt(r.value.childChemblIds.getOrElse(Seq.empty))
      )
    ),
    AddFields(
      Field(
        "drugWarnings",
        ListType(drugWarningsImp),
        description = Some("Warnings present on drug as identified by ChEMBL."),
        resolve = c => c.ctx.getDrugWarnings(c.value.id)
      ),
      Field(
        "similarEntities",
        ListType(similarityGQLImp),
        description = Some("Semantically similar drugs based on a PubMed word embedding model"),
        arguments = idsArg :: entityNames :: thresholdArg :: pageSize :: Nil,
        complexity = Some(complexityCalculator(pageSize)),
        resolve = c => {
          val ids = c.arg(idsArg).getOrElse(List.empty)
          val thres = c.arg(thresholdArg).getOrElse(0.1)
          val cats = c.arg(entityNames).getOrElse(Nil).toList
          val n = c.arg(pageSize).getOrElse(10)

          c.ctx.getSimilarW2VEntities(c.value.id, ids.toSet, cats, thres, n)
        }
      ),
      Field(
        "literatureOcurrences",
        publicationsImp,
        description = Some(
          "Return the list of publications that mention the main entity, " +
            "alone or in combination with other entities"
        ),
        arguments = idsArg :: startYear :: startMonth :: endYear :: endMonth :: cursor :: Nil,
        resolve = c => {
          val ids = c.arg(idsArg).getOrElse(List.empty) ++ List(c.value.id)
          val filterStartYear = c.arg(startYear)
          val filterStartMonth = c.arg(startMonth)
          val filterEndYear = c.arg(endYear)
          val filterEndMonth = c.arg(endMonth)
          val cur = c.arg(cursor)

          c.ctx.getLiteratureOcurrences(ids.toSet,
                                        filterStartYear,
                                        filterStartMonth,
                                        filterEndYear,
                                        filterEndMonth,
                                        cur
          )
        }
      ),
      Field(
        "mechanismsOfAction",
        OptionType(mechanismOfActionImp),
        description = Some(
          "Mechanisms of action to produce intended pharmacological effects. Curated from scientific " +
            "literature and post-marketing package inserts"
        ),
        resolve = ctx => ctx.ctx.getMechanismsOfAction(ctx.value.id)
      ),
      Field(
        "adverseEvents",
        OptionType(adverseEventsImp),
        description = Some(
          "Significant adverse events estimated from pharmacovigilance reports deposited in FAERS"
        ),
        arguments = pageArg :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = ctx => ctx.ctx.getAdverseEvents(ctx.value.id, ctx.arg(pageArg))
      ),
      Field(
        "pharmacogenomics",
        ListType(pharmacogenomicsImp),
        description = Some(
          "Pharmacogenomics data linking genetic variants to responses to this drug. Data is integrated from sources including ClinPGx and describes how genetic variants influence individual responses to this drug."
        ),
        arguments = pageArg :: Nil,
        complexity = Some(complexityCalculator(pageArg)),
        resolve = ctx => ctx.ctx.getPharmacogenomicsByDrug(ctx.value.id)
      ),
      Field(
        "indications",
        clinicalIndicationsFromDrugImp,
        description = Some(
          "Clinical indications for this drug as reported by clinical trial records."
        ),
        arguments = Nil,
        resolve = ctx => ctx.ctx.getClinicalIndicationsByDrug(ctx.value.id)
      )
    )
  )

  implicit val clinicalIndicationFromDiseaseImp: ObjectType[Backend, ClinicalIndication] =
    deriveObjectType[Backend, ClinicalIndication](
      ObjectTypeName("ClinicalIndicationFromDisease"),
      ExcludeFields("diseaseId"),
      ReplaceField(
        "drugId",
        Field(
          "drug",
          OptionType(drugImp),
          description = Some(
            ""
          ),
          resolve = ctx => {
            val id = ctx.value.drugId
            logger.debug(s"finding drug $id")
            drugsFetcher.deferOpt(id)
          }
        )
      )
    )

  implicit val clinicalIndicationFromDrugImp: ObjectType[Backend, ClinicalIndication] =
    deriveObjectType[Backend, ClinicalIndication](
      ObjectTypeName("ClinicalIndicationFromDrug"),
      ExcludeFields("drugId"),
      ReplaceField(
        "diseaseId",
        Field(
          "disease",
          OptionType(diseaseImp),
          description = Some(""),
          resolve = ctx =>
            ctx.value.diseaseId match {
              case Some(tId) =>
                logger.debug(s"finding disease $tId")
                diseasesFetcher.defer(tId)
              case None => None
            }
        )
      )
    )

  implicit val datasourceSettingsImp: ObjectType[Backend, DatasourceSettings] =
    deriveObjectType[Backend, DatasourceSettings](
      ObjectTypeDescription(
        "Datasource settings configuration used to compute target-disease associations. Allows customization of weights, ontology propagation, and required evidence for each datasource when calculating association scores. Weights must be between 0 and 1, and can control ontology propagation and evidence requirements."
      ),
      DocumentField("id", "Datasource identifier"),
      DocumentField("weight",
                    "Weight assigned to the datasource when computing association scores"
      ),
      DocumentField("propagate",
                    "Whether evidence from this datasource is propagated through the ontology"
      ),
      DocumentField(
        "required",
        "Whether evidence from this datasource is required to compute association scores"
      )
    )
  implicit val interactionSettingsImp: ObjectType[Backend, LUTableSettings] =
    deriveObjectType[Backend, LUTableSettings](
      ObjectTypeDescription("Lookup table settings configuration"),
      DocumentField("label", "Human-readable label for the lookup table"),
      DocumentField("name", "Name identifier for the lookup table"),
      DocumentField("key", "Key field used for lookups"),
      DocumentField("field", "Optional field name for additional lookup criteria")
    )
  implicit val dbSettingsImp: ObjectType[Backend, DbTableSettings] =
    deriveObjectType[Backend, DbTableSettings](
      ObjectTypeDescription("Database table settings configuration"),
      DocumentField("label", "Human-readable label for the database table"),
      DocumentField("name", "Name identifier for the database table")
    )
  implicit val targetSettingsImp: ObjectType[Backend, TargetSettings] =
    deriveObjectType[Backend, TargetSettings](
      ObjectTypeDescription("Target-specific database settings configuration"),
      DocumentField("label", "Human-readable label for target settings"),
      DocumentField("name", "Name identifier for target settings"),
      DocumentField("associations", "Database table settings for target associations")
    )
  implicit val diseaseSettingsImp: ObjectType[Backend, DiseaseSettings] =
    deriveObjectType[Backend, DiseaseSettings](
      ObjectTypeDescription("Disease-specific database settings configuration"),
      DocumentField("associations", "Database table settings for disease associations")
    )
  implicit val clinicalIndicationSettingsImp: ObjectType[Backend, ClinicalIndicationSettings] =
    deriveObjectType[Backend, ClinicalIndicationSettings](
      ObjectTypeDescription("Clinical indication database settings configuration"),
      DocumentField("drugTable", "Database table settings for drug indications"),
      DocumentField("diseaseTable", "Database table settings for disease indications")
    )
  implicit val harmonicSettingsImp: ObjectType[Backend, HarmonicSettings] =
    deriveObjectType[Backend, HarmonicSettings](
      ObjectTypeDescription("Harmonic mean scoring settings for association calculations"),
      DocumentField("pExponent", "Power exponent used in harmonic mean calculation"),
      DocumentField("datasources", "List of datasource settings with weights and propagation rules")
    )
  implicit val clickhouseSettingsImp: ObjectType[Backend, ClickhouseSettings] =
    deriveObjectType[Backend, ClickhouseSettings](
      ObjectTypeDescription("ClickHouse database configuration settings"),
      DocumentField("defaultDatabaseName", "Default database name for ClickHouse connections"),
      DocumentField("intervals", "Database table settings for genomic intervals"),
      DocumentField("target", "Target-specific database settings"),
      DocumentField("disease", "Disease-specific database settings"),
      DocumentField("similarities", "Database table settings for entity similarities"),
      DocumentField("harmonic", "Harmonic mean scoring settings"),
      DocumentField("literature", "Database table settings for literature data"),
      DocumentField("literatureIndex", "Database table settings for literature index"),
      DocumentField("clinicalIndication", "Database table settings for clinical indications")
    )
  implicit val evidenceSourceImp: ObjectType[Backend, EvidenceSource] =
    deriveObjectType[Backend, EvidenceSource](
      ObjectTypeDescription("Evidence datasource and datatype metadata"),
      DocumentField("datasource", "Name of the evidence datasource"),
      DocumentField(
        "datatype",
        "Datatype/category of the evidence (e.g., Genetic association, Somatic, Literature)"
      )
    )

  implicit val associatedOTFTargetsImp: ObjectType[Backend, Associations] =
    deriveObjectType[Backend, Associations](
      ObjectTypeName("AssociatedTargets"),
      ObjectTypeDescription(
        "Target-disease associations computed on-the-fly using configurable datasource weights and evidence filters. Returns associations with aggregated scores and evidence counts supporting the target-disease relationship."
      ),
      DocumentField("count",
                    "Total number of target-disease associations matching the query filters"
      ),
      DocumentField(
        "datasources",
        "List of datasource settings with weights and propagation rules used to compute the associations"
      ),
      ReplaceField(
        "rows",
        Field(
          "rows",
          ListType(associatedOTFTargetImp),
          Some("List of associated targets with their association scores and evidence breakdowns"),
          resolve = r => r.value.rows
        )
      )
    )

  implicit val associatedOTFDiseasesImp: ObjectType[Backend, Associations] =
    deriveObjectType[Backend, Associations](
      ObjectTypeName("AssociatedDiseases"),
      ObjectTypeDescription(
        "Target-disease associations computed on-the-fly using configurable datasource weights and evidence filters. Returns associations with aggregated scores and evidence counts supporting the target-disease relationship."
      ),
      DocumentField("count",
                    "Total number of target-disease associations matching the query filters"
      ),
      DocumentField(
        "datasources",
        "List of datasource settings with weights and propagation rules used to compute the associations"
      ),
      ReplaceField(
        "rows",
        Field(
          "rows",
          ListType(associatedOTFDiseaseImp),
          Some("List of associated diseases with their association scores and evidence breakdowns"),
          resolve = r => r.value.rows
        )
      )
    )
  implicit val geneOntologyTermImp: ObjectType[Backend, GeneOntologyTerm] =
    deriveObjectType[Backend, GeneOntologyTerm](
      ObjectTypeDescription("Gene ontology (GO) term [bioregistry:go]"),
      DocumentField("id", "Gene ontology term identifier [bioregistry:go]"),
      DocumentField("name", "Gene ontology term name")
    )

  lazy val mUnionType: UnionType[Backend] =
    UnionType(
      "EntityUnionType",
      description = Some(
        "Union of core Platform entities returned by search or mappings (Target, Drug, Disease, Variant, Study)"
      ),
      types = List(targetImp, drugImp, diseaseImp, variantIndexImp, studyImp)
    )

  implicit val searchResultAggsCategoryImp: ObjectType[Backend, SearchResultAggCategory] =
    deriveObjectType[Backend, models.entities.SearchResultAggCategory](
      ObjectTypeDescription("Search result aggregation category with result count"),
      DocumentField("name", "Category name (e.g., target, disease, drug)"),
      DocumentField("total", "Total number of search results in this category")
    )
  implicit val searchResultAggsEntityImp: ObjectType[Backend, SearchResultAggEntity] =
    deriveObjectType[Backend, models.entities.SearchResultAggEntity](
      ObjectTypeDescription("Search result aggregation by entity type with category breakdown"),
      DocumentField("name", "Entity type name (e.g., target, disease, drug, variant, study)"),
      DocumentField("total", "Total number of search results for this entity type"),
      DocumentField("categories", "List of category aggregations within this entity type")
    )
  implicit val searchResultAggsImp: ObjectType[Backend, SearchResultAggs] =
    deriveObjectType[Backend, models.entities.SearchResultAggs](
      ObjectTypeDescription("Search result aggregations grouped by entity type"),
      DocumentField("total", "Total number of search results across all entities"),
      DocumentField("entities", "List of entity type aggregations with category breakdowns")
    )
  implicit val searchResultImp: ObjectType[Backend, SearchResult] =
    deriveObjectType[Backend, models.entities.SearchResult](
      ObjectTypeDescription(
        "Full-text search hit describing a single entity and its relevance to the query"
      ),
      DocumentField("id", "Entity identifier (e.g., Ensembl, EFO, ChEMBL, variant or study ID)"),
      DocumentField("entity",
                    "Entity type of the hit (e.g., target, disease, drug, variant, study)"
      ),
      DocumentField("category",
                    "List of categories the hit belongs to (e.g., TARGET, DISEASE, DRUG)"
      ),
      DocumentField("name", "Primary display name for the entity"),
      DocumentField("description", "Short description or summary of the entity"),
      DocumentField("keywords", "Additional keywords associated with the entity to improve search"),
      DocumentField("multiplier",
                    "Score boosting multiplier applied to the hit during search ranking"
      ),
      DocumentField("prefixes", "List of name prefixes used for prefix matching"),
      DocumentField("ngrams", "List of n-grams derived from the name used for fuzzy matching"),
      DocumentField("score", "Relevance score returned by the search engine for this hit"),
      DocumentField("highlights", "Highlighted text snippets showing where the query matched"),
      AddFields(
        Field(
          "object",
          OptionType(mUnionType),
          description = Some("Resolved Platform entity corresponding to this search hit"),
          resolve = ctx =>
            ctx.value.entity match {
              case "target"  => targetsFetcher.deferOpt(ctx.value.id)
              case "disease" => diseasesFetcher.deferOpt(ctx.value.id)
              case "variant" => variantFetcher.deferOpt(ctx.value.id)
              case "study"   => studyFetcher.deferOpt(ctx.value.id)
              case _         => drugsFetcher.deferOpt(ctx.value.id)
            }
        )
      )
    )
  implicit val searchFacetsResultImp: ObjectType[Backend, SearchFacetsResult] =
    deriveObjectType[Backend, models.entities.SearchFacetsResult](
      ObjectTypeDescription("Facet search hit for a single category item"),
      DocumentField(
        "id",
        "Facet identifier, which can be inputted in the associations query to filter by this facet"
      ),
      DocumentField("label", "Human-readable facet label"),
      DocumentField("category", "Facet category this item belongs to (e.g., target, disease)"),
      DocumentField("entityIds",
                    "Optional list of underlying entity identifiers represented by this facet"
      ),
      DocumentField("datasourceId",
                    "Optional identifier of the datasource contributing this facet"
      ),
      DocumentField("score", "Relevance score of the facet hit for the current query"),
      DocumentField("highlights",
                    "Highlighted text snippets showing why this facet matched the query"
      )
    )

  implicit val similarityGQLImp: ObjectType[Backend, Similarity] =
    deriveObjectType[Backend, models.entities.Similarity](
      ObjectTypeDescription(
        "Semantic similarity score between labels, used to suggest related entities"
      ),
      DocumentField("category",
                    "Entity category this similarity refers to (e.g., target, disease, drug)"
      ),
      DocumentField("id", "Identifier of the similar entity (e.g., Ensembl, EFO, ChEMBL ID)"),
      DocumentField(
        "score",
        "Similarity score between this entity and the query label. Scores are normalised between 0 and 1; higher scores indicate more similar entities."
      ),
      AddFields(
        Field(
          "object",
          OptionType(mUnionType),
          description = Some("Resolved Platform entity corresponding to this similar label"),
          resolve = ctx =>
            ctx.value.category match {
              case "target"  => targetsFetcher.deferOpt(ctx.value.id)
              case "disease" => diseasesFetcher.deferOpt(ctx.value.id)
              case _         => drugsFetcher.deferOpt(ctx.value.id)
            }
        )
      )
    )

  implicit val searchResultsImp: ObjectType[Backend, SearchResults] =
    deriveObjectType[Backend, models.entities.SearchResults](
      ObjectTypeDescription("Full-text search results with total hit count and aggregations"),
      DocumentField("total", "Total number of hits for the search query"),
      DocumentField("aggregations", "Facet aggregations over entities and categories"),
      DocumentField("hits", "List of search hits matching the query")
    )

  implicit val mappingResultImp: ObjectType[Backend, MappingResult] =
    deriveObjectType[Backend, MappingResult](
      ObjectTypeDescription("Mapping result for a single input term"),
      DocumentField("term", "Input term submitted for mapping"),
      DocumentField("hits", "Search hits that the term maps to, if any")
    )

  implicit val mappingResultsImp: ObjectType[Backend, MappingResults] =
    deriveObjectType[Backend, MappingResults](
      ObjectTypeDescription(
        "Mapping results for multiple terms with total hit count and aggregations"
      ),
      DocumentField("total", "Total number of mapped hits across all terms"),
      DocumentField("aggregations", "Facet aggregations over mapped entities and categories"),
      ReplaceField(
        "mappings",
        Field(
          "mappings",
          ListType(mappingResultImp),
          description = Some("Per-term mapping results"),
          resolve = _.value.mappings
        )
      )
    )

  implicit val searchFacetsCategoryImp: ObjectType[Backend, SearchFacetsCategory] =
    deriveObjectType[Backend, SearchFacetsCategory](
      ObjectTypeDescription("Facet category with result count"),
      DocumentField("name", "Facet category name"),
      DocumentField("total", "Number of results in this category")
    )

  val searchResultsGQLImp: ObjectType[Backend, SearchResults] = ObjectType(
    "SearchResults",
    "Search results including hits and facet aggregations",
    fields[Backend, SearchResults](
      Field(
        "aggregations",
        OptionType(searchResultAggsImp),
        description = Some("Facet aggregations by entity and category for the current query"),
        resolve = _.value.aggregations
      ),
      Field(
        "hits",
        ListType(searchResultImp),
        description = Some("Combined list of search hits across requested entities"),
        resolve = _.value.hits
      ),
      Field(
        "total",
        LongType,
        description = Some("Total number of results for the current query and entity filter"),
        resolve = _.value.total
      )
    )
  )

  val searchFacetsResultsGQLImp: ObjectType[Backend, SearchFacetsResults] = ObjectType(
    "SearchFacetsResults",
    "Facet search results including hits and category counts",
    fields[Backend, SearchFacetsResults](
      Field(
        "hits",
        ListType(searchFacetsResultImp),
        description = Some("List of facetable hits matching the query"),
        resolve = _.value.hits
      ),
      Field(
        "total",
        LongType,
        description = Some("Total number of facetable results for the current query"),
        resolve = _.value.total
      ),
      Field(
        "categories",
        ListType(searchFacetsCategoryImp),
        description = Some("Facet categories with their result counts"),
        resolve = _.value.categories
      )
    )
  )

  implicit val variantEffectImp: ObjectType[Backend, VariantEffect] =
    deriveObjectType[Backend, VariantEffect](
      ObjectTypeDescription("Predicted or measured effect of the variant based on various methods"),
      DocumentField(
        "method",
        "Method used to predict or measure the variant effect (e.g. VEP, SIFT, GERP, AlphaMissense, FoldX)"
      ),
      DocumentField("assessment", "Assessment of the variant effect"),
      DocumentField("score", "Score indicating the severity or impact of the variant effect"),
      DocumentField("assessmentFlag", "Flag indicating the reliability of the assessment"),
      DocumentField("normalisedScore", "Normalised score for the variant effect"),
      ReplaceField(
        "targetId",
        Field(
          "target",
          OptionType(targetImp),
          description =
            Some("The target (gene/protein) on which the variant effect is interpreted"),
          resolve = r => targetsFetcher.deferOpt(r.value.targetId)
        )
      )
    )
  implicit val transcriptConsequenceImp: ObjectType[Backend, TranscriptConsequence] =
    deriveObjectType[Backend, TranscriptConsequence](
      ObjectTypeDescription("Predicted consequences of the variant on transcript context"),
      DocumentField("aminoAcidChange", "Amino acid change caused by the variant"),
      DocumentField("uniprotAccessions",
                    "UniProt protein accessions for the transcript [bioregistry:uniprot]"
      ),
      DocumentField("isEnsemblCanonical",
                    "Whether this is the canonical transcript according to Ensembl"
      ),
      DocumentField("codons", "Codons affected by the variant"),
      DocumentField("distanceFromFootprint", "Distance from the variant to the footprint region"),
      DocumentField("distanceFromTss", "Distance from the variant to the transcription start site"),
      DocumentField("impact", "Impact assessment of the variant (e.g., HIGH, MODERATE, LOW)"),
      DocumentField("transcriptId", "Ensembl transcript identifier [bioregistry:ensembl]"),
      DocumentField("lofteePrediction",
                    "Loss-of-function transcript effect estimator (LOFTEE) prediction"
      ),
      DocumentField("siftPrediction",
                    "SIFT score predicting whether the variant affects protein function"
      ),
      DocumentField("polyphenPrediction",
                    "PolyPhen score predicting the impact of the variant on protein structure"
      ),
      DocumentField("transcriptIndex", "Index of the transcript"),
      DocumentField("consequenceScore", "Score indicating the severity of the consequence"),
      ReplaceField(
        "targetId",
        Field(
          "target",
          OptionType(targetImp),
          description = Some("The target (gene/protein) associated with the transcript"),
          resolve = r => targetsFetcher.deferOpt(r.value.targetId)
        )
      ),
      ReplaceField(
        "variantFunctionalConsequenceIds",
        Field(
          "variantConsequences",
          ListType(sequenceOntologyTermImp),
          description = Some(
            "The sequence ontology term of the consequence of the variant based on Ensembl VEP in the context of the transcript"
          ),
          resolve = r =>
            r.value.variantFunctionalConsequenceIds match {
              case Some(ids) =>
                val soIds = ids.map(_.replace("_", ":"))
                logger.debug(s"finding variant functional consequences", keyValue("ids", soIds))
                soTermsFetcher.deferSeqOpt(soIds)
              case None => Future.successful(Seq.empty)
            }
        )
      )
    )
  implicit val alleleFrequencyImp: ObjectType[Backend, AlleleFrequency] =
    deriveObjectType[Backend, AlleleFrequency](
      ObjectTypeDescription("Allele frequency of the variant in different populations"),
      DocumentField("populationName",
                    "Name of the population where the allele frequency was measured"
      ),
      DocumentField("alleleFrequency",
                    "Frequency of the allele in the population (ranging from 0 to 1)"
      )
    )
  implicit val biosampleImp: ObjectType[Backend, Biosample] = deriveObjectType[Backend, Biosample](
    ObjectTypeDescription(
      "Integration of biosample metadata about tissues or cell types derived from multiple ontologies including EFO, UBERON, CL, GO and others."
    ),
    DocumentField("biosampleId", "Unique identifier for the biosample"),
    DocumentField("biosampleName", "Name of the biosample"),
    DocumentField("description", "Description of the biosample"),
    DocumentField("xrefs", "Cross-reference IDs from other ontologies"),
    DocumentField("synonyms", "List of synonymous names for the term"),
    DocumentField("parents", "Direct parent biosample IDs in the ontology"),
    DocumentField("ancestors", "List of ancestor biosample IDs in the ontology"),
    DocumentField("children", "Direct child biosample IDs in the ontology"),
    DocumentField("descendants", "List of descendant biosample IDs in the ontology")
  )
  implicit val l2GFeatureImp: ObjectType[Backend, L2GFeature] =
    deriveObjectType[Backend, L2GFeature](
      ObjectTypeDescription("Feature used in Locus2gene model predictions"),
      DocumentField("name", "Name of the feature"),
      DocumentField("value", "Value of the feature"),
      DocumentField(
        "shapValue",
        "SHAP (SHapley Additive exPlanations) value indicating the feature's contribution to the prediction"
      )
    )
  implicit val l2GPredictionImp: ObjectType[Backend, L2GPrediction] =
    deriveObjectType[Backend, L2GPrediction](
      ObjectTypeDescription(
        "Predictions from Locus2gene model integrating multiple functional genomic features to estimate the most likely causal gene for a given credible set. The dataset contains all predictions for every combination of credible set and genes in the region as well as statistics to explain the model interpretation of the predictions."
      ),
      DocumentField("studyLocusId", "Study-locus identifier for the credible set"),
      DocumentField(
        "score",
        "Locus2gene prediction score for the gene assignment. Higher scores indicate a stronger association between the credible set and the gene. Scores range from 0 to 1."
      ),
      DocumentField("features", "Features used in the Locus2gene model prediction"),
      DocumentField(
        "shapBaseValue",
        "SHAP base value for the prediction. This value is common to all predictions for a given credible set."
      ),
      ReplaceField(
        "geneId",
        Field(
          "target",
          OptionType(targetImp),
          description = Some("Target entity of the L2G predicted gene"),
          resolve = r => targetsFetcher.deferOpt(r.value.geneId)
        )
      )
    )
  implicit val l2GPredictionsImp: ObjectType[Backend, L2GPredictions] =
    deriveObjectType[Backend, L2GPredictions](
      ObjectTypeDescription(
        "Predictions from Locus2gene gene assignment model. The dataset contains all predictions for every combination of credible set and genes in the region as well as statistics to explain the model interpretation of the predictions."
      ),
      DocumentField("id", "Study-locus identifier for the credible set"),
      DocumentField("count", "Total number of Locus2gene predictions"),
      DocumentField("rows", "List of Locus2gene predictions for credible set and gene combinations")
    )
  implicit val proteinCodingEvidenceSourceImp: ObjectType[Backend, Datasource] =
    deriveObjectType[Backend, Datasource](
      ObjectTypeDescription("Data source information for protein coding coordinates"),
      DocumentField("datasourceId", "Identifier of the data source"),
      DocumentField("datasourceNiceName", "Human-readable name of the data source"),
      DocumentField("datasourceCount", "Count of evidence from this data source")
    )
  implicit val proteinCodingCoordinateImp: ObjectType[Backend, ProteinCodingCoordinate] =
    deriveObjectType[Backend, ProteinCodingCoordinate](
      ObjectTypeDescription(
        "Descriptions of variant consequences at protein level. Protein coding coordinates link variants to their amino acid-level consequences in protein products."
      ),
      DocumentField("uniprotAccessions",
                    "UniProt protein accessions for the affected protein [bioregistry:uniprot]"
      ),
      DocumentField("aminoAcidPosition",
                    "Position of the amino acid affected by the variant in the protein sequence"
      ),
      DocumentField("alternateAminoAcid", "Amino acid resulting from the variant"),
      DocumentField("referenceAminoAcid", "Reference amino acid at this position"),
      DocumentField("variantEffect",
                    "Score indicating the predicted effect of the variant on the protein"
      ),
      DocumentField("therapeuticAreas",
                    "Therapeutic areas associated with the variant-consequence relationship"
      ),
      DocumentField("datasources",
                    "Data sources providing evidence for the protein coding coordinate"
      ),
      ReplaceField(
        "diseases",
        Field(
          "diseases",
          ListType(diseaseImp),
          description = Some("Disease the protein coding variant has been associated with"),
          resolve = r => diseasesFetcher.deferSeqOpt(r.value.diseases)
        )
      ),
      ReplaceField(
        "targetId",
        Field(
          "target",
          OptionType(targetImp),
          description =
            Some("Target (gene/protein) the protein coding variant has been associated with"),
          resolve = r => targetsFetcher.deferOpt(r.value.targetId)
        )
      ),
      ReplaceField(
        "variantId",
        Field(
          "variant",
          OptionType(variantIndexImp),
          description = Some("Protein coding variant"),
          resolve = r => variantFetcher.deferOpt(r.value.variantId)
        )
      ),
      ReplaceField(
        "variantFunctionalConsequenceIds",
        Field(
          "variantConsequences",
          ListType(sequenceOntologyTermImp),
          description = Some(
            "The sequence ontology term capturing the consequence of the variant based on Ensembl VEP in the context of the transcript [bioregistry:so]"
          ),
          resolve = r =>
            r.value.variantFunctionalConsequenceIds match {
              case Some(ids) =>
                val soIds = ids.map(_.replace("_", ":"))
                logger.debug(s"finding variant functional consequences", keyValue("id", soIds))
                soTermsFetcher.deferSeqOpt(soIds)
              case None => Future.successful(Seq.empty)
            }
        )
      )
    )
  implicit val proteinCodingCoordinatesImp: ObjectType[Backend, ProteinCodingCoordinates] =
    deriveObjectType[Backend, ProteinCodingCoordinates](
      ObjectTypeDescription(
        "Collection of protein coding coordinates linking variants to their amino acid-level consequences"
      ),
      DocumentField("count", "Total number of phenotype-associated protein coding variants"),
      DocumentField("rows", "List of phenotype-associated protein coding variants")
    )
  implicit val colocalisationImp: ObjectType[Backend, Colocalisation] =
    deriveObjectType[Backend, Colocalisation](
      ObjectTypeDescription(
        "GWAS-GWAS and GWAS-molQTL credible set colocalisation results. Dataset includes colocalising pairs as well as the method and statistics used to estimate the colocalisation."
      ),
      DocumentField("rightStudyType", "Type of the right-side study (e.g., gwas, eqtl, pqtl)"),
      DocumentField("chromosome", "Chromosome where the colocalisation occurs"),
      DocumentField("colocalisationMethod",
                    "Method used to estimate colocalisation (e.g., coloc, eCAVIAR)"
      ),
      DocumentField("numberColocalisingVariants",
                    "Number of variants intersecting between two overlapping study-loci"
      ),
      DocumentField(
        "h3",
        "Posterior probability that both traits are associated, but with different causal variants (H3). Used in coloc method."
      ),
      DocumentField(
        "h4",
        "Posterior probability that both traits are associated and share a causal variant (H4). Used in coloc method."
      ),
      DocumentField(
        "clpp",
        "Colocalisation posterior probability (CLPP) score estimating the probability of shared causal variants. Used in eCAVIAR method."
      ),
      DocumentField("betaRatioSignAverage",
                    "Average sign of the beta ratio between colocalised variants"
      ),
      ReplaceField(
        "otherStudyLocusId",
        Field(
          "otherStudyLocus",
          OptionType(credibleSetImp),
          description = Some("The other credible set (study-locus) in the colocalisation pair"),
          resolve = r =>
            val studyLocusId = r.value.otherStudyLocusId.getOrElse("")
            logger.debug(s"finding colocalisation credible set", keyValue("id", studyLocusId))
            credibleSetFetcher.deferOpt(studyLocusId)
        )
      )
    )

  implicit val dbXrefImp: ObjectType[Backend, DbXref] = deriveObjectType[Backend, DbXref](
    ObjectTypeDescription("Cross-reference information for a variant in different databases"),
    DocumentField("id", "Identifier of the variant in the given database"),
    DocumentField("source", "Name of the database the variant is referenced in")
  )
  implicit val variantIndexImp: ObjectType[Backend, VariantIndex] =
    deriveObjectType[Backend, VariantIndex](
      ObjectTypeName("Variant"),
      ObjectTypeDescription(
        "Core variant information for all variants in the Platform. Variants are included if any phenotypic information is available for the variant, including GWAS or molQTL credible sets, ClinVar, Uniprot or ClinPGx. The dataset includes variant metadata as well as variant effects derived from Ensembl VEP."
      ),
      DocumentField(
        "variantId",
        "The unique identifier for the variant following schema CHR_POS_REF_ALT for SNPs and short indels (e.g. 1_154453788_C_T)"
      ),
      DocumentField("chromosome", "The chromosome on which the variant is located"),
      DocumentField("position", "The position on the chromosome of the variant"),
      DocumentField("referenceAllele", "The reference allele for the variant"),
      DocumentField("alternateAllele", "The alternate allele for the variant"),
      DocumentField("variantEffect",
                    "List of predicted or measured effects of the variant based on various methods"
      ),
      DocumentField("transcriptConsequences", "Predicted consequences on transcript context"),
      DocumentField("rsIds", "The list of rsId identifiers for the variant"),
      DocumentField("dbXrefs",
                    "The list of cross-references for the variant in different databases"
      ),
      DocumentField("alleleFrequencies",
                    "The allele frequencies of the variant in different populations"
      ),
      DocumentField("hgvsId", "HGVS identifier of the variant"),
      DocumentField("variantDescription", "Short summary of the variant effect"),
      ReplaceField(
        "mostSevereConsequenceId",
        Field(
          "mostSevereConsequence",
          OptionType(sequenceOntologyTermImp),
          description = Some(
            "The sequence ontology term of the most severe consequence of the variant based on Ensembl VEP"
          ),
          resolve = r =>
            val soId = (r.value.mostSevereConsequenceId)
              .replace("_", ":")
            logger.debug(s"finding variant functional consequence", keyValue("id", soId))
            soTermsFetcher.deferOpt(soId)
        )
      ),
      AddFields(
        Field(
          "credibleSets",
          credibleSetsImp,
          description = Some(
            "95% credible sets for GWAS and molQTL studies that contain this variant. Credible sets include all variants in the credible set (locus) as well as the fine-mapping method and derived statistics."
          ),
          arguments = pageArg :: studyTypes :: Nil,
          complexity = Some(complexityCalculator(pageArg)),
          resolve =
            r => CredibleSetsByVariantDeferred(r.value.variantId, r.arg(studyTypes), r.arg(pageArg))
        ),
        Field(
          "pharmacogenomics",
          ListType(pharmacogenomicsImp),
          description = Some(
            "Pharmacogenomics data linking this genetic variant to drug responses. Data is integrated from sources including ClinPGx and describes how genetic variants influence individual drug responses."
          ),
          arguments = pageArg :: Nil,
          complexity = Some(complexityCalculator(pageArg)),
          resolve = ctx => ctx.ctx.getPharmacogenomicsByVariant(ctx.value.variantId)
        ),
        Field(
          "evidences",
          evidencesImp,
          description = Some(
            "Target-disease evidence from all data sources where this variant supports the association. Evidence entries report associations between targets (genes or proteins) and diseases or phenotypes, scored according to confidence in the association."
          ),
          arguments = datasourceIdsArg :: pageSize :: cursor :: Nil,
          complexity = Some(complexityCalculator(pageSize)),
          resolve = ctx =>
            ctx.ctx.getEvidencesByVariantId(
              ctx arg datasourceIdsArg,
              ctx.value.variantId,
              Some(("score", "desc")),
              ctx arg pageSize,
              ctx arg cursor
            )
        ),
        Field(
          "proteinCodingCoordinates",
          proteinCodingCoordinatesImp,
          description = Some(
            "Protein coding coordinates linking this variant to its amino acid-level consequences in protein products. Describes variant consequences at the protein level including amino acid changes and their positions."
          ),
          arguments = pageArg :: Nil,
          complexity = Some(complexityCalculator(pageArg)),
          resolve = ctx =>
            ctx.ctx.getProteinCodingCoordinatesByVariantId(ctx.value.variantId, ctx.arg(pageArg))
        ),
        Field(
          "intervals",
          intervalsImp,
          description = Some(
            "Regulatory enhancer/promoter regions to gene (target) predictions overlapping with this variant's location. These intervals link regulatory regions to target genes based on experimental data for specific tissues or cell types."
          ),
          arguments = pageArg :: Nil,
          complexity = Some(complexityCalculator(pageArg)),
          resolve = ctx =>
            ctx.ctx.getIntervals(ctx.value.chromosome,
                                 ctx.value.position,
                                 ctx.value.position,
                                 ctx.arg(pageArg)
            )
        )
      ),
      RenameField("variantId", "id")
    )

  implicit val nameAndDescriptionImp: ObjectType[Backend, NameAndDescription] =
    deriveObjectType[Backend, NameAndDescription](
      ObjectTypeName("NameDescription"),
      ObjectTypeDescription("Generic pair of a name and its description"),
      DocumentField("name", "Name or label of the element"),
      DocumentField("description", "Human-readable description of the element")
    )

  implicit val pathwayTermImp: ObjectType[Backend, PathwayTerm] =
    deriveObjectType[Backend, PathwayTerm](
      ObjectTypeName("Pathway"),
      ObjectTypeDescription("Pathway metadata from Reactome pathway database."),
      DocumentField("id", "Reactome pathway identifier [bioregistry:reactome]"),
      DocumentField("name", "Reactome pathway name")
    )

  implicit val evidenceTextMiningSentenceImp: ObjectType[Backend, EvidenceTextMiningSentence] =
    deriveObjectType[Backend, EvidenceTextMiningSentence](
      ObjectTypeName("EvidenceTextMiningSentence"),
      ObjectTypeDescription(
        "Extracted text snippet from literature supporting a target–disease statement"
      ),
      DocumentField("section",
                    "Publication section where the sentence was found (e.g., abstract, results)"
      ),
      DocumentField("text", "Sentence text supporting the association"),
      DocumentField("tStart", "Start character offset of the target mention in the sentence"),
      DocumentField("tEnd", "End character offset of the target mention in the sentence"),
      DocumentField("dStart", "Start character offset of the disease mention in the sentence"),
      DocumentField("dEnd", "End character offset of the disease mention in the sentence")
    )

  implicit val evidenceDiseaseCellLineImp: ObjectType[Backend, EvidenceDiseaseCellLine] =
    deriveObjectType[Backend, EvidenceDiseaseCellLine](
      ObjectTypeName("DiseaseCellLine"),
      ObjectTypeDescription("Cancer cell lines used to generate evidence"),
      DocumentField("id", "Cell type identifier in cell ontology or in cell model database"),
      DocumentField("name", "Name of the cell model"),
      DocumentField("tissue", "Name of the tissue from which the cells were sampled"),
      DocumentField("tissueId", "Anatomical identifier of the sampled organ/tissue")
    )

  implicit val evidenceVariationImp: ObjectType[Backend, EvidenceVariation] =
    deriveObjectType[Backend, EvidenceVariation](
      ObjectTypeName("EvidenceVariation"),
      ObjectTypeDescription("Summary of mutation counts by functional consequence in the cohort"),
      DocumentField(
        "numberMutatedSamples",
        "Number of cohort samples in which the target is mutated with a mutation of any type"
      ),
      DocumentField("numberSamplesTested", "Number of cohort samples tested"),
      DocumentField(
        "numberSamplesWithMutationType",
        "Number of cohort samples in which the target is mutated with a specific mutation type"
      ),
      ReplaceField(
        "functionalConsequenceId",
        Field(
          "functionalConsequence",
          OptionType(sequenceOntologyTermImp),
          description = Some(
            "Sequence ontology (SO) identifier of the functional consequence of the variant [bioregistry:so]"
          ),
          resolve = js => {
            val soId = js.value.functionalConsequenceId.map(_.replace("_", ":"))
            soTermsFetcher.deferOpt(soId)
          }
        )
      )
    )

  implicit val labelledElementImp: ObjectType[Backend, LabelledElement] =
    deriveObjectType[Backend, LabelledElement](
      ObjectTypeName("LabelledElement"),
      ObjectTypeDescription("Identifier and human-readable label pair"),
      DocumentField("id", "Identifier value"),
      DocumentField("label", "Human-readable label")
    )

  implicit val labelledUriImp: ObjectType[Backend, LabelledUri] =
    deriveObjectType[Backend, LabelledUri](
      ObjectTypeName("LabelledUri"),
      ObjectTypeDescription("External resource link with an optional display name"),
      DocumentField("url", "URL to the external resource"),
      DocumentField("niceName", "Optional human-readable label for the URL")
    )

  implicit val biomarkerGeneExpressionImp: ObjectType[Backend, BiomarkerGeneExpression] =
    deriveObjectType[Backend, BiomarkerGeneExpression](
      ObjectTypeName("BiomarkerGeneExpression"),
      ObjectTypeDescription("List of gene expression altering biomarkers"),
      DocumentField("name", "Raw gene expression annotation from the source"),
      ReplaceField(
        "id",
        Field(
          "id",
          OptionType(geneOntologyTermImp),
          description = Some(
            "Gene Ontology (GO) identifiers of regulation or background expression processes [bioregistry:go]"
          ),
          resolve = js => {
            val goId = js.value.id.map(_.replace('_', ':'))
            goFetcher.deferOpt(goId)
          }
        )
      )
    )

  implicit val biomarkerVariantImp: ObjectType[Backend, BiomarkerVariant] = deriveObjectType(
    ObjectTypeName("geneticVariation"),
    ObjectTypeDescription("List of genetic variation biomarkers"),
    DocumentField("id", "Variation identifier"),
    DocumentField("name", "Name of the variant biomarker"),
    ReplaceField(
      "functionalConsequenceId",
      Field(
        "functionalConsequenceId",
        OptionType(sequenceOntologyTermImp),
        description =
          Some("Functional consequence identifier of the variant biomarker [bioregistry:so]"),
        resolve = js => {
          val soId = js.value.functionalConsequenceId.map(_.replace("_", ":"))
          soTermsFetcher.deferOpt(soId)
        }
      )
    )
  )

  implicit val biomarkersImp: ObjectType[Backend, Biomarkers] = deriveObjectType(
    ObjectTypeName("biomarkers"),
    ObjectTypeDescription("List of biomarkers associated with evidence"),
    DocumentField("geneExpression", "List of gene expression altering biomarkers"),
    DocumentField("geneticVariation", "List of genetic variation biomarkers")
  )

  implicit val assaysImp: ObjectType[Backend, Assays] = deriveObjectType(
    ObjectTypeName("assays"),
    ObjectTypeDescription("Assays used in the study"),
    DocumentField("description", "Description of the assay"),
    DocumentField("isHit", "Indicating if the assay was positive or negative for the target"),
    DocumentField("shortName", "Short name of the assay")
  )

  implicit val evidenceImp: ObjectType[Backend, Evidence] = deriveObjectType(
    ObjectTypeName("Evidence"),
    ObjectTypeDescription(
      "Target - disease evidence from all data sources. Every piece of evidence supporting an association between a target (gene or protein) and a disease or phenotype is reported and scored according to the confidence we have in the association. Multiple target-disease evidence from the same source can be reported in this dataset. The dataset is partitioned by data source, therefore evidence for individual sources can be retrieved separately. The dataset schema is a superset of all the schemas for all sources."
    ),
    DocumentField("id", "Identifer of the disease/target evidence"),
    DocumentField("score",
                  "Score of the evidence reflecting the strength of the disease/target relationship"
    ),
    DocumentField("datasourceId", "Identifer of the evidence source"),
    DocumentField("datatypeId", "Type of the evidence"),
    DocumentField("biomarkerName", "Altered characteristics that influences the disease process"),
    DocumentField("biomarkers", "List of biomarkers"),
    DocumentField("diseaseCellLines", "Cancer cell lines used to generate evidence"),
    DocumentField("cohortPhenotypes",
                  "Clinical features/phenotypes observed in studied individuals"
    ),
    DocumentField("targetInModel", "Target name/synonym in animal model"),
    DocumentField("reactionId", "Pathway, gene set or reaction identifier in Reactome"),
    DocumentField("reactionName", "Name of the reaction, patway or gene set in Reactome"),
    DocumentField("projectId", "The identifer of the project that generated the data"),
    DocumentField("variantRsId", "Variant reference SNP cluster ID (Rsid)"),
    DocumentField("oddsRatioConfidenceIntervalLower",
                  "Lower value of the confidence interval for odds ratio"
    ),
    DocumentField("oddsRatioConfidenceIntervalUpper",
                  "Upper value of the confidence interval for odds ratio"
    ),
    DocumentField("oddsRatio", "Size of effect captured as odds ratio"),
    DocumentField("studySampleSize", "Sample size of study"),
    DocumentField("variantAminoacidDescriptions",
                  "Descriptions of variant consequences at protein level"
    ),
    DocumentField("mutatedSamples", "Samples with a given mutation tested"),
    DocumentField("drugFromSource", "Drug name/family in resource of origin"),
    DocumentField("cohortShortName", "Short name of the studied cohort"),
    DocumentField("cohortDescription", "Description of the studied cohort"),
    DocumentField("cohortId", "Identifier of the studied cohort"),
    DocumentField("diseaseModelAssociatedModelPhenotypes",
                  "Phenotypes observed in genetically-modified animal models"
    ),
    DocumentField("diseaseModelAssociatedHumanPhenotypes",
                  "Human phenotypes equivalent to those observed in animal models"
    ),
    DocumentField("significantDriverMethods",
                  "Methods to detect cancer driver genes producing significant results"
    ),
    DocumentField("pValueExponent", "Exponent of the p-value"),
    DocumentField("pValueMantissa", "Mantissa of the p-value"),
    DocumentField("log2FoldChangePercentileRank",
                  "Percentile of top differentially regulated genes (transcripts) within experiment"
    ),
    DocumentField("log2FoldChangeValue", "Log2 fold expression change in contrast experiment"),
    DocumentField("biologicalModelAllelicComposition", "Allelic composition of the model organism"),
    DocumentField("confidence", "Confidence qualifier on the reported evidence"),
    DocumentField(
      "clinicalPhase",
      "Phase of the clinical trial. [Values: -1: `Unknown`, 0: `Phase 0`, 0.5: `Phase I (Early)`, 1: `Phase I`, 2: `Phase II`, 3: `Phase III`, 4: `Phase IV`]"
    ),
    DocumentField("clinicalStatus", "Current stage of a clinical study"),
    DocumentField("clinicalSignificances", "Standard terms to define clinical significance"),
    DocumentField("resourceScore",
                  "Score provided by datasource indicating strength of target-disease association"
    ),
    DocumentField("biologicalModelGeneticBackground", "Genetic background of the model organism"),
    DocumentField(
      "urls",
      "Reference to linked external resource (e.g. clinical trials, studies, package inserts, reports, etc.)"
    ),
    DocumentField("literature", "List of PubMed or preprint reference identifiers"),
    DocumentField("studyCases", "Number of cases in case-control study"),
    DocumentField("studyOverview", "Description of the study"),
    DocumentField("studyId", "Identifier of the study generating the data"),
    DocumentField("pathways", "List of pooled pathways"),
    DocumentField("allelicRequirements", "Inheritance patterns"),
    DocumentField("alleleOrigins", "Origin of the variant allele"),
    DocumentField("publicationYear", "Year of the publication"),
    DocumentField(
      "publicationFirstAuthor",
      "Last name and initials of the first author of the publication that references the evidence"
    ),
    DocumentField("diseaseFromSource", "Disease label from the original source"),
    DocumentField("diseaseFromSourceId", "Disease identifier from the original source"),
    DocumentField("diseaseFromSourceMappedId", "Mapped Open Targets disease identifier"),
    DocumentField(
      "targetFromSourceId",
      "Target ID in resource of origin (accepted sources include Ensembl gene ID, Uniprot ID, gene symbol), only capital letters are accepted"
    ),
    DocumentField("targetFromSource",
                  "Target name/synonym or non HGNC symbol in resource of origin"
    ),
    DocumentField("targetModulation", "Description of target modulation event"),
    DocumentField("textMiningSentences", "Text mining sentences extracted from literature"),
    DocumentField("biologicalModelId", "Identifier of the biological model (eg. in MGI)"),
    DocumentField("biosamplesFromSource", "Identifier of the referenced biological material"),
    DocumentField("beta", "Effect size of numberic traits"),
    DocumentField("betaConfidenceIntervalLower", "Lower value of the confidence interval"),
    DocumentField("betaConfidenceIntervalUpper", "Upper value of the confidence interval"),
    DocumentField("studyStartDate", "Start date of study in a YYYY-MM-DD format"),
    DocumentField("studyStopReason", "Reason why a study has been stopped"),
    DocumentField("studyStopReasonCategories",
                  "Predicted reason(s) why the study has been stopped based on studyStopReason"
    ),
    DocumentField("cellLineBackground", "Background of the derived cell lines"),
    DocumentField("contrast", "Experiment contrast"),
    DocumentField("crisprScreenLibrary",
                  "The applied screening library in the CRISPR/CAS9 project"
    ),
    DocumentField("cellType", "The studied cell type. Preferably the cell line ontology label"),
    DocumentField("statisticalTestTail", "End of the distribution the target was picked from"),
    DocumentField("interactingTargetFromSourceId", "Identifer of the interacting target"),
    DocumentField("phenotypicConsequenceLogFoldChange", "Log 2 fold change of the cell survival"),
    DocumentField("phenotypicConsequenceFDR", "False discovery rate of the genetic test"),
    DocumentField("phenotypicConsequencePValue", "P-value of the the cell survival test"),
    DocumentField(
      "geneticInteractionScore",
      "The strength of the genetic interaction. Directionality is captured as well: antagonistics < 0 < cooperative"
    ),
    DocumentField("geneticInteractionPValue", "P-value of the genetic interaction test"),
    DocumentField("geneticInteractionFDR", "False discovery rate of the genetic interaction test"),
    DocumentField("biomarkerList", "List of biomarkers associated with the biological model"),
    DocumentField("projectDescription", "Description of the project that generated the data"),
    DocumentField("geneInteractionType", "Description of the interaction between the two genes"),
    DocumentField("targetRole", "Role of a target in the genetic interaction test"),
    DocumentField("interactingTargetRole", "Role of a target in the genetic interaction test"),
    DocumentField("assays", "Assays used in the study"),
    DocumentField("ancestry", "Genetic origin of a population"),
    DocumentField("ancestryId",
                  "Identifier of the ancestry in the HANCESTRO ontology [bioregistry:hancestro]"
    ),
    DocumentField("statisticalMethod", "The statistical method used to calculate the association"),
    DocumentField("statisticalMethodOverview",
                  "Overview of the statistical method used to calculate the association"
    ),
    DocumentField(
      "studyCasesWithQualifyingVariants",
      "Number of cases in a case-control study that carry at least one allele of the qualifying variant"
    ),
    DocumentField("releaseVersion", "Open Targets data release version"),
    DocumentField("releaseDate", "Date of the release of the data in a 'YYYY-MM-DD' format"),
    DocumentField("warningMessage", "Warning message"),
    DocumentField(
      "directionOnTarget",
      "Gain or loss of function effect of the evidence on the target resulting from genetic variants, pharmacological modulation, or other perturbations"
    ),
    DocumentField("directionOnTrait", "Predicted direction of effect on the trait"),
    DocumentField(
      "assessments",
      "Assessment of a study. In the context of Validation Lab, this is the assessment of the validation gene in a given cellular context"
    ),
    DocumentField("primaryProjectHit",
                  "If a given target was found to be a hit in the primary project"
    ),
    DocumentField("primaryProjectId", "Open Targets project identifier of the primary project"),
    DocumentField("directionOnTrait", "Direction On Trait"),
    DocumentField("assessments", "Assessments"),
    DocumentField("primaryProjectHit", "Primary Project Hit"),
    DocumentField("primaryProjectId", "Primary Project Id"),
    ReplaceField(
      "targetId",
      Field(
        "target",
        targetImp,
        description = Some("Target for which the disease is associated in this evidence"),
        resolve = evidence => {
          val tId = evidence.value.targetId
          targetsFetcher.defer(tId)
        }
      )
    ),
    ReplaceField(
      "diseaseId",
      Field(
        "disease",
        diseaseImp,
        description = Some("Disease for which the target is associated in this evidence"),
        resolve = evidence => {
          val tId = evidence.value.diseaseId
          diseasesFetcher.defer(tId)
        }
      )
    ),
    ReplaceField(
      "studyLocusId",
      Field(
        "credibleSet",
        OptionType(credibleSetImp),
        description = Some("Credible set (StudyLocus) supporting this evidence"),
        resolve = js => {
          val studyLocusId = js.value.studyLocusId
          credibleSetFetcher.deferOpt(studyLocusId)
        }
      )
    ),
    ReplaceField(
      "variantId",
      Field(
        "variant",
        OptionType(variantIndexImp),
        description =
          Some("Variant supporting the relationship between the target and the disease"),
        resolve = evidence => {
          val id = evidence.value.variantId
          logger.debug(s"finding variant", keyValue("id", id))
          variantFetcher.deferOpt(id)
        }
      )
    ),
    ReplaceField(
      "drugId",
      Field(
        "drug",
        OptionType(drugImp),
        description = Some(
          "Drug or clinical candidate targeting the target and studied/approved for the specific disease as potential indication [bioregistry:chembl]"
        ),
        resolve = evidence => {
          val id = evidence.value.drugId
          logger.debug(s"finding drug", keyValue("id", id))
          drugsFetcher.deferOpt(id)
        }
      )
    ),
    ReplaceField(
      "drugResponse",
      Field(
        "drugResponse",
        OptionType(diseaseImp),
        description = Some("Observed patterns of drug response"),
        resolve = evidence => {
          val id = evidence.value.drugResponse
          logger.debug(s"finding drug", keyValue("id", id))
          diseasesFetcher.deferOpt(id)
        }
      )
    ),
    ReplaceField(
      "variantFunctionalConsequenceId",
      Field(
        "variantFunctionalConsequence",
        OptionType(sequenceOntologyTermImp),
        description =
          Some("Sequence ontology (SO) term of the functional consequence of the variant"),
        resolve = evidence => {
          val soId = evidence.value.variantFunctionalConsequenceId
            .map(id => id.replace("_", ":"))
          logger.debug(s"finding variant functional consequence", keyValue("id", soId))
          soTermsFetcher.deferOpt(soId)
        }
      )
    ),
    ReplaceField(
      "variantFunctionalConsequenceFromQtlId",
      Field(
        "variantFunctionalConsequenceFromQtlId",
        OptionType(sequenceOntologyTermImp),
        description =
          Some("Sequence ontology (SO) term of the functional consequence of the variant from QTL"),
        resolve = evidence => {
          val soId = evidence.value.variantFunctionalConsequenceFromQtlId
            .map(id => id.replace("_", ":"))
          soTermsFetcher.deferOpt(soId)
        }
      )
    ),
    ReplaceField(
      "pmcIds",
      Field(
        "pubMedCentralIds",
        OptionType(ListType(StringType)),
        description =
          Some("List of PubMed Central identifiers of full text publication [bioregistry:pmc]"),
        resolve = js => js.value.pmcIds
      )
    )
  )

  implicit val ldSetImp: ObjectType[Backend, LdSet] =
    deriveObjectType[Backend, LdSet](
      ObjectTypeDescription(
        "Variants in linkage disequilibrium (LD) with the credible set lead variant."
      ),
      DocumentField("tagVariantId",
                    "The variant ID for tag variants in LD with the credible set lead variant"
      ),
      DocumentField("r2Overall",
                    "The R-squared value for the tag variants with the credible set lead variant"
      )
    )

  implicit val locusImp: ObjectType[Backend, Locus] = deriveObjectType[Backend, Locus](
    ObjectTypeDescription("List of variants within the credible set"),
    DocumentField("posteriorProbability",
                  "Posterior inclusion probability for the variant within this credible set"
    ),
    DocumentField("pValueMantissa", "Mantissa of the P-value for this variant in the credible set"),
    DocumentField("pValueExponent", "Exponent of the P-value for this variant in the credible set"),
    DocumentField("logBF", "Log (natural) Bayes factor for the variant from fine-mapping"),
    DocumentField("beta", "Beta coefficient of this variant in the credible set"),
    DocumentField("standardError", "Standard error of this variant in the credible set"),
    DocumentField("is95CredibleSet", "Boolean for if the variant is part of the 95% credible set"),
    DocumentField("is99CredibleSet", "Boolean for if the variant is part of the 99% credible set"),
    DocumentField("r2Overall",
                  "R-squared (LD) between this credible set variant and the lead variant"
    ),
    ReplaceField(
      "variantId",
      Field(
        "variant",
        OptionType(variantIndexImp),
        description = Some("Variant in the credible set"),
        resolve = r => {
          val variantId = r.value.variantId.getOrElse("")
          logger.debug(s"finding variant index", keyValue("id", variantId))
          variantFetcher.deferOpt(variantId)
        }
      )
    )
  )

  implicit val lociImp: ObjectType[Backend, Loci] = deriveObjectType[Backend, Loci](
    ObjectTypeDescription("Collection of variants within a credible set (locus)"),
    ExcludeFields("id"),
    DocumentField("count", "Total number of variants in the credible set"),
    DocumentField("rows", "Variants within the credible set and their associated statistics")
  )

  implicit val credibleSetImp: ObjectType[Backend, CredibleSet] =
    deriveObjectType[Backend, CredibleSet](
      ObjectTypeName("CredibleSet"),
      ObjectTypeDescription(
        "95% credible sets for GWAS and molQTL studies. Credible sets include all variants in the credible set (locus) as well as the fine-mapping method and derived statistics."
      ),
      DocumentField("studyLocusId", "Identifier of the credible set (StudyLocus)"),
      DocumentField(
        "studyId",
        "Identifier of the GWAS or molQTL study in which the credible set was identified"
      ),
      DocumentField("chromosome", "Chromosome which the credible set is located"),
      DocumentField("position", "Position of the lead variant for the credible set (GRCh38)"),
      DocumentField("region", "Start and end positions of the region used for fine-mapping"),
      DocumentField("beta", "Beta coefficient of the lead variant"),
      DocumentField("zScore", "Z-score of the lead variant from the GWAS"),
      DocumentField("pValueMantissa", "Mantissa value of the lead variant P-value"),
      DocumentField("pValueExponent", "Exponent value of the lead variant P-value"),
      DocumentField("effectAlleleFrequencyFromSource",
                    "Allele frequency of the lead variant from the GWAS"
      ),
      DocumentField("standardError", "Standard error of the lead variant"),
      DocumentField("subStudyDescription", "[Deprecated]"),
      DocumentField("qualityControls", "Quality control flags for this credible set"),
      DocumentField("finemappingMethod", "Method used for fine-mapping of credible set"),
      DocumentField("credibleSetIndex",
                    "Integer label for the order of credible sets from study-region"
      ),
      DocumentField("credibleSetlog10BF", "Log10 Bayes factor for the entire credible set"),
      DocumentField("purityMeanR2",
                    "Mean R-squared linkage disequilibrium for variants in the credible set"
      ),
      DocumentField("purityMinR2",
                    "Minimum R-squared linkage disequilibrium for variants in the credible set"
      ),
      DocumentField("locusStart",
                    "Start position of the region that was fine-mapped for this credible set"
      ),
      DocumentField("locusEnd",
                    "End position of the region that was fine-mapped for this credible set"
      ),
      DocumentField("sampleSize", "Sample size of the study which this credible set is derived"),
      DocumentField(
        "ldSet",
        "Array of structs which denote the variants in LD with the credible set lead variant"
      ),
      DocumentField(
        "qtlGeneId",
        "Ensembl identifier of the gene representing a specific gene whose molecular is being analysed in molQTL study"
      ),
      DocumentField(
        "confidence",
        "Description of how this credible set was derived in terms of data and fine-mapping method"
      ),
      DocumentField("isTransQtl", "Boolean for whether this credible set is a trans-pQTL or not"),
      ReplaceField(
        "variantId",
        Field(
          "variant",
          OptionType(variantIndexImp),
          description = Some("The lead variant for the credible set, by posterior probability."),
          resolve = js => {
            val id = js.value.variantId
            logger.debug(s"finding variant", keyValue("id", id))
            variantFetcher.deferOpt(id)
          }
        )
      ),
      ReplaceField(
        "studyType",
        Field(
          "studyType",
          OptionType(StudyType),
          description =
            Some("Descriptor for whether the credible set is derived from GWAS or molecular QTL."),
          resolve = js => js.value.studyType
        )
      ),
      AddFields(
        Field(
          "l2GPredictions",
          l2GPredictionsImp,
          description = Some("Predictions from Locus2gene gene assignment model."),
          arguments = pageArg :: Nil,
          complexity = Some(complexityCalculator(pageArg)),
          resolve = js => {
            val id: String = js.value.studyLocusId
            L2GPredictionsDeferred(id, js.arg(pageArg))
          }
        ),
        Field(
          "locus",
          lociImp,
          arguments = variantIds :: pageArg :: Nil,
          description = Some("Locus information for all variants in the credible set"),
          complexity = Some(complexityCalculator(pageArg)),
          resolve = js => {
            import scala.concurrent.ExecutionContext.Implicits.global
            val id = js.value.studyLocusId
            LocusDeferred(id, js.arg(variantIds), js.arg(pageArg))
          }
        ),
        Field(
          "colocalisation",
          colocalisationsImp,
          description = Some(
            "GWAS-GWAS and GWAS-molQTL credible set colocalisation results. Dataset includes colocalising pairs as well as the method and statistics used to estimate the colocalisation."
          ),
          arguments = studyTypes :: pageArg :: Nil,
          complexity = Some(complexityCalculator(pageArg)),
          resolve = js => {
            val id = js.value.studyLocusId
            ColocalisationsDeferred(id, js.arg(studyTypes), js.arg(pageArg))
          }
        ),
        Field(
          "study",
          OptionType(studyImp),
          description = Some("GWAS or molQTL study in which the credible set was identified"),
          resolve = js => {
            val studyId = js.value.studyId
            logger.debug(s"finding gwas study", keyValue("id", studyId))
            studyFetcher.deferOpt(studyId)
          }
        )
      )
    )
  implicit val ldPopulationStructureImp: ObjectType[Backend, LdPopulationStructure] =
    deriveObjectType[Backend, LdPopulationStructure](
      ObjectTypeDescription(
        "Collection of populations referenced by the study. Used to describe the linkage disequilibrium (LD) population structure of GWAS studies."
      ),
      DocumentField("ldPopulation", "Population identifier"),
      DocumentField("relativeSampleSize",
                    "Fraction of the total sample represented by the population"
      )
    )
  implicit val sampleImp: ObjectType[Backend, Sample] = deriveObjectType[Backend, Sample](
    ObjectTypeDescription(
      "Sample information including ancestry and sample size. Used for both discovery and replication phases of GWAS studies."
    ),
    DocumentField("ancestry", "Sample ancestry name"),
    DocumentField("sampleSize", "Sample size")
  )
  implicit val sumStatQCImp: ObjectType[Backend, SumStatQC] = deriveObjectType[Backend, SumStatQC](
    ObjectTypeDescription(
      "Quality control flags for summary statistics. Mapping of quality control metric names to their corresponding values."
    ),
    DocumentField("QCCheckName", "Quality control metric identifier"),
    DocumentField("QCCheckValue", "Quality control metric value")
  )

  implicit val studyImp: ObjectType[Backend, Study] = deriveObjectType(
    ObjectTypeName("Study"),
    ObjectTypeDescription(
      "Metadata for all complex trait and molecular QTL GWAS studies in the Platform. The dataset includes study metadata, phenotype information, sample sizes, publication information and more. Molecular QTL studies are splitted by the affected gene, tissue or cell type and condition, potentially leading to many studies in the same publication."
    ),
    DocumentField("condition", "Reported sample conditions"),
    DocumentField(
      "projectId",
      "Identifier of the source project collection that the study information is derived from"
    ),
    DocumentField("traitFromSource",
                  "Molecular or phenotypic trait, derived from source, analysed in the study"
    ),
    DocumentField("traitFromSourceMappedIds",
                  "Phenotypic trait ids that map to the analysed trait reported by study"
    ),
    DocumentField("nSamples", "The number of samples tested in GWAS analysis"),
    DocumentField("summarystatsLocation",
                  "Path to the source study summary statistics (if exists at the source)"
    ),
    DocumentField("hasSumstats", "Indication whether the summary statistics exist in the source"),
    DocumentField("cohorts", "List of cohort(s) represented in the discovery sample"),
    DocumentField("initialSampleSize", "Study initial sample size"),
    DocumentField("publicationJournal",
                  "Abbreviated journal name where the publication referencing study was published"
    ),
    DocumentField("publicationDate", "Date of the publication that references study"),
    DocumentField(
      "pubmedId",
      "PubMed identifier of the publication hat references the study [bioregistry:pubmed]"
    ),
    DocumentField(
      "publicationFirstAuthor",
      "Last name and initials of the author of the publication that references the study"
    ),
    DocumentField("publicationTitle", "Title of the publication that references the study"),
    DocumentField("qualityControls", "Control metrics refining study validation"),
    DocumentField("nControls", "The number of controls in this broad ancestry group"),
    DocumentField("nCases", "The number of cases in this broad ancestry group"),
    DocumentField(
      "analysisFlags",
      "Collection of flags indicating the type of the analysis conducted in the association study"
    ),
    DocumentField("ldPopulationStructure", "Collection of populations referenced by the study"),
    DocumentField("discoverySamples",
                  "Collection of ancestries reported by the study discovery phase"
    ),
    DocumentField("replicationSamples",
                  "Collection of ancestries reported by the study replication phase"
    ),
    DocumentField("sumstatQCValues", "Quality control flags for the study (if any)"),
    ReplaceField(
      "studyId",
      Field(
        "id",
        StringType,
        description = Some("The GWAS or molQTL study identifier (e.g. GCST004132)"),
        resolve = js => js.value.studyId
      )
    ),
    ReplaceField(
      "studyType",
      Field(
        "studyType",
        OptionType(StudyType),
        description = Some("The study type (e.g. gwas, eqtl, pqtl, sceqtl)"),
        resolve = js => js.value.studyType
      )
    ),
    ReplaceField(
      "geneId",
      Field(
        "target",
        OptionType(targetImp),
        Some("In molQTL studies, the gene under study for changes in expression, abundance, etc."),
        resolve = js => {
          val geneId = js.value.geneId
          logger.debug(s"finding target", keyValue("id", geneId))
          targetsFetcher.deferOpt(geneId)
        }
      )
    ),
    ReplaceField(
      "biosampleFromSourceId",
      Field(
        "biosample",
        OptionType(biosampleImp),
        Some("Tissue or cell type in which the molQTL has been detected"),
        resolve = js => {
          val biosampleId = js.value.biosampleFromSourceId
          biosamplesFetcher.deferOpt(biosampleId)
        }
      )
    ),
    ReplaceField(
      "diseaseIds",
      Field(
        "diseases",
        OptionType(ListType(diseaseImp)),
        Some("Phenotypic trait ids that map to the analysed trait reported by study"),
        resolve = js => {
          val ids = js.value.diseaseIds.getOrElse(Seq.empty)
          logger.debug(s"finding diseases", keyValue("ids", ids))
          diseasesFetcher.deferSeqOpt(ids)
        }
      )
    ),
    ReplaceField(
      "backgroundTraitFromSourceMappedIds",
      Field(
        "backgroundTraits",
        OptionType(ListType(diseaseImp)),
        Some("Any background trait(s) shared by all individuals in the study"),
        resolve = js => {
          val ids = js.value.backgroundTraitFromSourceMappedIds
            .getOrElse(Seq.empty)
          logger.debug(s"finding diseases", keyValue("ids", ids))
          diseasesFetcher.deferSeqOpt(ids)
        }
      )
    ),
    AddFields(
      Field(
        "credibleSets",
        credibleSetsImp,
        arguments = pageArg :: Nil,
        description = Some(
          "95% credible sets for GWAS and molQTL studies. Credible sets include all variants in the credible set as well as the fine-mapping method and statistics used to estimate the credible set."
        ),
        complexity = Some(complexityCalculator(pageArg)),
        resolve = js => {
          val studyId = js.value.studyId
          CredibleSetsByStudyDeferred(studyId, js.arg(pageArg))
        }
      )
    )
  )

  implicit val interactionEvidencePDMImp: ObjectType[Backend, InteractionEvidencePDM] =
    deriveObjectType[Backend, InteractionEvidencePDM](
      ObjectTypeDescription("Detection method used to identify participants in the interaction"),
      DocumentField(
        "miIdentifier",
        "Molecular Interactions (MI) identifier for the detection method [bioregistry:mi]"
      ),
      DocumentField("shortName", "Short name of the detection method")
    )

  implicit val interactionSpeciesImp: ObjectType[Backend, InteractionSpecies] =
    deriveObjectType[Backend, InteractionSpecies](
      ObjectTypeDescription("Taxonomic annotation of the interaction participants"),
      DocumentField("mnemonic", "Short mnemonic name of the species"),
      DocumentField("scientificName", "Scientific name of the species"),
      DocumentField("taxonId", "NCBI taxon ID of the species")
    )

  implicit val interactionResources: ObjectType[Backend, InteractionResources] =
    deriveObjectType[Backend, InteractionResources](
      ObjectTypeDescription("Databases providing evidence for the interaction"),
      DocumentField("sourceDatabase",
                    "Name of the source database reporting the interaction evidence"
      ),
      DocumentField("databaseVersion",
                    "Version of the source database providing interaction evidence"
      )
    )

  implicit val interactionEvidenceImp: ObjectType[Backend, InteractionEvidence] =
    deriveObjectType[Backend, InteractionEvidence](
      ObjectTypeDescription(
        "Evidence supporting molecular interactions between targets. Contains detailed information about how the interaction was detected, the experimental context, and supporting publications."
      ),
      DocumentField("evidenceScore",
                    "Score indicating the confidence or strength of the interaction evidence"
      ),
      DocumentField(
        "expansionMethodMiIdentifier",
        "Molecular Interactions (MI) identifier for the expansion method used [bioregistry:mi]"
      ),
      DocumentField("expansionMethodShortName",
                    "Short name of the method used to expand the interaction dataset"
      ),
      DocumentField("hostOrganismScientificName",
                    "Scientific name of the host organism in which the interaction was observed"
      ),
      DocumentField("hostOrganismTaxId", "NCBI taxon ID of the host organism"),
      DocumentField("intASource", "Source where interactor A is identified"),
      DocumentField("intBSource", "Source where interactor B is identified"),
      DocumentField(
        "interactionDetectionMethodMiIdentifier",
        "Molecular Interactions (MI) identifier for the interaction detection method [bioregistry:mi]"
      ),
      DocumentField("interactionDetectionMethodShortName",
                    "Short name of the method used to detect the interaction"
      ),
      DocumentField("interactionIdentifier",
                    "Unique identifier for the interaction evidence entry at the source"
      ),
      DocumentField(
        "interactionTypeMiIdentifier",
        "Molecular Interactions (MI) identifier for the type of interaction [bioregistry:mi]"
      ),
      DocumentField("interactionTypeShortName", "Short name of the interaction type"),
      DocumentField("participantDetectionMethodA",
                    "Detection method used to identify participant A in the interaction"
      ),
      DocumentField("participantDetectionMethodB",
                    "Detection method used to identify participant B in the interaction"
      ),
      DocumentField(
        "pubmedId",
        "PubMed ID of the publication supporting the interaction evidence [bioregistry:pubmed]"
      )
    )

  implicit val interactionImp: ObjectType[Backend, Interaction] =
    deriveObjectType[Backend, Interaction](
      ObjectTypeDescription(
        "Integration of molecular interactions reporting experimental or functional interactions between molecules represented as Platform targets. This dataset contains pair-wise interactions deposited in several databases capturing: physical interactions (e.g. IntAct), directional interactions (e.g. Signor), pathway relationships (e.g. Reactome) or functional interactions (e.g. STRINGdb)."
      ),
      DocumentField("intA", "Identifier for target A in source"),
      DocumentField("intB", "Identifier for target B in source"),
      DocumentField("intABiologicalRole", "Biological role of target A in the interaction"),
      DocumentField("intBBiologicalRole", "Biological role of target B in the interaction"),
      DocumentField("count", "Number of evidence entries supporting this interaction"),
      DocumentField("sourceDatabase", "Name of the source database reporting the interaction"),
      DocumentField("speciesA", "Taxonomic annotation of target A"),
      DocumentField("speciesB", "Taxonomic annotation of target B"),
      RenameField("scoring", "score"),
      DocumentField(
        "scoring",
        "Scoring or confidence value assigned to the interaction. Scores are normalized to a range of 0-1. The higher the score, the stronger the support for the interaction. In IntAct, scores are captured with the MI score."
      ),
      ReplaceField(
        "targetA",
        Field(
          "targetA",
          OptionType(targetImp),
          description =
            Some("Target (gene/protein) of the first molecule (target A) in the interaction"),
          resolve = interaction => {
            val tId = interaction.value.targetA
            targetsFetcher.deferOpt(tId)
          }
        )
      ),
      ReplaceField(
        "targetB",
        Field(
          "targetB",
          OptionType(targetImp),
          description =
            Some("Target (gene/protein) of the second molecule (target B) in the interaction"),
          resolve = interaction => {
            val tId = interaction.value.targetB
            targetsFetcher.deferOpt(tId)
          }
        )
      ),
      AddFields(
        Field(
          "evidences",
          ListType(interactionEvidenceImp),
          description = Some("List of evidences for this interaction"),
          resolve = r => {
            import scala.concurrent.ExecutionContext.Implicits.global
            import r.ctx._

            val ev = r.value
            Interaction.findEvidences(ev)
          }
        )
      )
    )
}
