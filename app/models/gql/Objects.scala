package models.gql

import models._
import models.entities.Configuration._
import models.entities.Evidences._
import models.entities.Interactions._
import models.entities.Publications.publicationsImp
import models.entities._
import models.gql.Arguments._
import models.gql.Fetchers._
import play.api.Logging
import sangria.macros.derive.{DocumentField, _}
import sangria.schema._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

object Objects extends Logging {
  implicit val metaDataVersionImp = deriveObjectType[Backend, DataVersion]()
  implicit val metaAPIVersionImp = deriveObjectType[Backend, APIVersion]()
  implicit val metaImp = deriveObjectType[Backend, Meta]()

  implicit lazy val targetImp: ObjectType[Backend, Target] = deriveObjectType(
    ObjectTypeDescription("Target entity"),
    DocumentField("id", "Open Targets target id"),
    DocumentField("approvedSymbol", "HGNC approved symbol"),
    DocumentField("approvedName", "Approved gene name"),
    DocumentField("biotype", "Molecule biotype"),
    DocumentField("dbXrefs", "Database cross references"),
    DocumentField("functionDescriptions", "..."), // todo
    DocumentField("geneticConstraint", "Symbol synonyms"),
    DocumentField("genomicLocation", "Chromosomic location"),
    DocumentField("geneOntology", "Gene Ontology annotations"),
    DocumentField("hallmarks",
      "Target-modulated essential alterations in cell physiology that dictate " +
        "malignant growth"),
    DocumentField("homologues", "Gene homologues"),
    DocumentField("proteinIds", "Related protein IDs"),
    //    DocumentField("safetyLiabilities", "Known target safety effects and target safety risk information"),
    DocumentField("subcellularLocations", "Location of ..."), // todo
    DocumentField("synonyms", "Alternative names and symbols"),
    DocumentField("tep", "Target Enabling Package (TEP)"),
    DocumentField("tractability", "Target druggability assessment"),
    DocumentField("transcriptIds", "Ensembl transcript IDs"),
    DocumentField("pathways", "Reactome pathways"),
    AddFields(
      Field(
        "similarEntities",
        ListType(similarityGQLImp),
        description = Some("Return similar labels using a model Word2CVec trained with PubMed"),
        arguments = idsArg :: entityNames :: thresholdArg :: pageSize :: Nil,
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
            "alone or in combination with other entities"),
        arguments = idsArg :: cursor :: Nil,
        resolve = c => {
          val ids = c.arg(idsArg).getOrElse(List.empty) ++ List(c.value.id)
          val cur = c.arg(cursor)

          c.ctx.getLiteratureOcurrences(ids.toSet, cur)
        }
      ),
      Field(
        "evidences",
        evidencesImp,
        description = Some("The complete list of all possible datasources"),
        arguments = efoIds :: datasourceIdsArg :: pageSize :: cursor :: Nil,
        resolve = ctx => {
          ctx.ctx.getEvidences(ctx arg datasourceIdsArg,
                               Seq(ctx.value.id),
                               ctx arg efoIds,
                               Some(("score", "desc")),
                               ctx arg pageSize,
                               ctx arg cursor)
        }
      ),
      Field(
        "interactions",
        OptionType(interactions),
        description = Some("Biological pathway membership from Reactome"),
        arguments = databaseName :: pageArg :: Nil,
        resolve = r => {
          import r.ctx._

          Interactions.find(r.value.id, r arg databaseName, r arg pageArg)
        }
      ),
      Field(
        "mousePhenotypes",
        ListType(mouseGeneImp),
        description = Some("Biological pathway membership from Reactome"),
        resolve = r =>
          DeferredValue(mousePhenotypeFetcher.deferOpt(r.value.id)).map {
            case Some(mouseGenes) => mouseGenes.rows
            case None             => Seq.empty
        }
      ),
      Field(
        "expressions",
        ListType(expressionImp),
        description = Some("RNA and Protein baseline expression"),
        resolve = r =>
          DeferredValue(expressionFetcher.deferOpt(r.value.id)).map {
            case Some(expressions) => expressions.rows
            case None              => Seq.empty
        }
      ),
      Field(
        "knownDrugs",
        OptionType(knownDrugsImp),
        description = Some(
          "Clinical precedence for drugs with investigational or approved indications " +
            "targeting gene products according to their curated mechanism of action"),
        arguments = freeTextQuery :: pageSize :: cursor :: Nil,
        resolve = ctx => {
          ctx.ctx.getKnownDrugs(ctx.arg(freeTextQuery).getOrElse(""),
                                Map("targetId.raw" -> ctx.value.id),
                                ctx.arg(pageSize),
                                ctx.arg(cursor))
        }
      ),
      Field(
        "cancerBiomarkers",
        OptionType(cancerBiomarkersImp),
        description = Some(
          "Clinical relevance and drug responses of tumor genomic alterations " +
            "on the target"),
        arguments = pageArg :: Nil,
        resolve = ctx => ctx.ctx.getCancerBiomarkers(ctx.value.id, ctx.arg(pageArg))
      ),
      Field(
        "associatedDiseases",
        associatedOTFDiseasesImp,
        description = Some("associations on the fly"),
        arguments = BIds :: indirectEvidences :: datasourceSettingsListArg :: aggregationFiltersListArg :: BFilterString :: scoreSorting :: pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getAssociationsTargetFixed(
            ctx.value,
            ctx arg datasourceSettingsListArg,
            ctx arg indirectEvidences getOrElse false,
            ctx arg aggregationFiltersListArg getOrElse Seq.empty,
            ctx arg BIds map (_.toSet) getOrElse Set.empty,
            ctx arg BFilterString,
            (ctx arg scoreSorting) map (_.split(" ").take(2).toList match {
              case a :: b :: Nil => (a, b)
              case a :: Nil => (a, "desc")
              case _ => ("score", "desc")
            }),
            ctx arg pageArg
          )
      ),
    )
  )

  implicit lazy val chemicalProbeUrlImp = deriveObjectType[Backend, ChemicalProbeUrl]()
  implicit lazy val chemicalProbeImp = deriveObjectType[Backend, ChemicalProbe]()

  implicit lazy val reactomePathwayImp = deriveObjectType[Backend, ReactomePathway]()
  // disease
  implicit lazy val diseaseSynonymsImp = deriveObjectType[Backend, DiseaseSynonyms]()

  implicit lazy val diseaseImp: ObjectType[Backend, Disease] = deriveObjectType(
    ObjectTypeDescription("Disease or phenotype entity"),
    DocumentField("id", "Open Targets disease id"),
    DocumentField("name", "Disease name"),
    DocumentField("description", "Disease description"),
    DocumentField("synonyms", "Disease synonyms"),
    DocumentField("dbXRefs", "List of external cross reference IDs"),
    ExcludeFields("ontology"),
    DocumentField("obsoleteTerms", "List of obsolete diseases"),
    DocumentField("directLocationIds", "List of direct location Disease terms"),
    DocumentField("indirectLocationIds", "List of indirect location Disease terms"),
    ReplaceField(
      "therapeuticAreas",
      Field(
        "therapeuticAreas",
        ListType(diseaseImp),
        Some("Ancestor therapeutic area disease entities in ontology"),
        resolve = r => diseasesFetcher.deferSeq(r.value.therapeuticAreas)
      )
    ),
    ReplaceField("parents",
                 Field("parents",
                       ListType(diseaseImp),
                       Some("Disease parents entities in ontology"),
                       resolve = r => diseasesFetcher.deferSeq(r.value.parents))),
    ReplaceField("children",
                 Field("children",
                       ListType(diseaseImp),
                       Some("Disease children entities in ontology"),
                       resolve = r => diseasesFetcher.deferSeq(r.value.children))),
    AddFields(
      Field(
        "directLocations",
        ListType(diseaseImp),
        Some("Direct Location disease terms"),
        resolve = r => diseasesFetcher.deferSeqOpt(r.value.directLocationIds.getOrElse(Seq.empty))
      ),
      Field(
        "indirectLocations",
        ListType(diseaseImp),
        Some("Indirect Location disease terms"),
        resolve = r => diseasesFetcher.deferSeqOpt(r.value.indirectLocationIds.getOrElse(Seq.empty))
      ),
      Field(
        "similarEntities",
        ListType(similarityGQLImp),
        description = Some("Return similar labels using a model Word2CVec trained with PubMed"),
        arguments = idsArg :: entityNames :: thresholdArg :: pageSize :: Nil,
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
            "alone or in combination with other entities"),
        arguments = idsArg :: cursor :: Nil,
        resolve = c => {
          val ids = c.arg(idsArg).getOrElse(List.empty) ++ List(c.value.id)
          val cur = c.arg(cursor)

          c.ctx.getLiteratureOcurrences(ids.toSet, cur)
        }
      ),
      Field(
        "isTherapeuticArea",
        BooleanType,
        description = Some("Is disease a therapeutic area itself"),
        resolve = ctx => ctx.value.ontology.isTherapeuticArea
      ),
      Field(
        "phenotypes",
        OptionType(diseaseHPOsImp),
        description = Some("Phenotype from HPO index"),
        arguments = pageArg :: Nil,
        resolve = ctx => ctx.ctx.getDiseaseHPOs(ctx.value.id, ctx.arg(pageArg))
      ),
      Field(
        "evidences",
        evidencesImp,
        description = Some("The complete list of all possible datasources"),
        arguments = ensemblIds :: indirectEvidences :: datasourceIdsArg :: pageSize :: cursor :: Nil,
        resolve = ctx => {
          val indirects = ctx.arg(indirectEvidences).getOrElse(true)
          val efos = if (indirects) ctx.value.id +: ctx.value.descendants else ctx.value.id +: Nil
          ctx.ctx.getEvidences(ctx arg datasourceIdsArg,
                               ctx arg ensemblIds,
                               efos,
                               Some(("score", "desc")),
                               ctx arg pageSize,
                               ctx arg cursor)
        }
      ),
      Field(
        "otarProjects",
        ListType(otarProjectImp),
        description = Some("RNA and Protein baseline expression"),
        resolve = r =>
          DeferredValue(otarProjectsFetcher.deferOpt(r.value.id)).map {
            case Some(otars) => otars.rows
            case None        => Seq.empty
        }
      ),
      Field(
        "knownDrugs",
        OptionType(knownDrugsImp),
        description = Some(
          "Clinical precedence for investigational or approved " +
            "drugs indicated for disease and curated mechanism of action"),
        arguments = freeTextQuery :: pageSize :: cursor :: Nil,
        resolve = ctx => {
          ctx.ctx.getKnownDrugs(
            ctx.arg(freeTextQuery).getOrElse(""),
            Map(
              "diseaseId.raw" -> ctx.value.id,
              "ancestors.raw" -> ctx.value.id
            ),
            ctx.arg(pageSize),
            ctx.arg(cursor)
          )
        }
      ),
      Field(
        "associatedTargets",
        associatedOTFTargetsImp,
        description = Some("associations on the fly"),
        arguments = BIds :: indirectEvidences :: datasourceSettingsListArg :: aggregationFiltersListArg :: BFilterString :: scoreSorting :: pageArg :: Nil,
        resolve = ctx =>
          ctx.ctx.getAssociationsDiseaseFixed(
            ctx.value,
            ctx arg datasourceSettingsListArg,
            ctx arg indirectEvidences getOrElse (true),
            ctx arg aggregationFiltersListArg getOrElse (Seq.empty),
            ctx arg BIds map (_.toSet) getOrElse (Set.empty),
            ctx arg BFilterString,
            (ctx arg scoreSorting) map (_.split(" ").take(2).toList match {
              case a :: b :: Nil => (a, b)
              case a :: Nil      => (a, "desc")
              case _             => ("score", "desc")
            }),
            ctx arg pageArg
        )
      )
    )
  )

  implicit val tractabilityImp = deriveObjectType[Backend, Tractability]()

  implicit val scoredDataTypeImp = deriveObjectType[Backend, ScoredComponent]()

  implicit val associatedOTFTargetImp = deriveObjectType[Backend, Association](
    ObjectTypeName("AssociatedTarget"),
    ObjectTypeDescription("Associated Target Entity"),
    ReplaceField(
      "id",
      Field("target", targetImp, Some("Target"), resolve = r => targetsFetcher.defer(r.value.id)))
  )

  implicit val associatedOTFDiseaseImp = deriveObjectType[Backend, Association](
    ObjectTypeName("AssociatedDisease"),
    ObjectTypeDescription("Associated Disease Entity"),
    ReplaceField("id",
                 Field("disease",
                       diseaseImp,
                       Some("Disease"),
                       resolve = r => diseasesFetcher.defer(r.value.id)))
  )

  implicit val ecoImp = deriveObjectType[Backend, ECO](
    ObjectTypeDescription("Evidence & Conclusion Ontology (ECO) annotation"),
    DocumentField("id", "ECO term id"),
    DocumentField("label", "ECO term label")
  )

  implicit lazy val reactomeImp: ObjectType[Backend, Reactome] =
    deriveObjectType[Backend, Reactome](
      AddFields(
        Field("isRoot",
              BooleanType,
              description = Some("If the node is root"),
              resolve = _.value.isRoot)
      ),
      ReplaceField("children",
                   Field("children",
                         ListType(reactomeImp),
                         Some("Reactome Nodes"),
                         resolve = r => reactomeFetcher.deferSeqOpt(r.value.children))),
      ReplaceField("parents",
                   Field("parents",
                         ListType(reactomeImp),
                         Some("Reactome Nodes"),
                         resolve = r => reactomeFetcher.deferSeqOpt(r.value.parents))),
      ReplaceField("ancestors",
                   Field("ancestors",
                         ListType(reactomeImp),
                         Some("Reactome Nodes"),
                         resolve = r => reactomeFetcher.deferSeqOpt(r.value.ancestors)))
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
    DocumentField("meddraCode", "8 digit unique meddra identification number"),
    DocumentField("count", "Number of reports mentioning drug and adverse event"),
    DocumentField("logLR", "Log-likelihood ratio"),
    ExcludeFields("criticalValue")
  )

  implicit val adverseEventsImp = deriveObjectType[Backend, AdverseEvents](
    ObjectTypeDescription("Significant adverse events inferred from FAERS reports"),
    DocumentField("count", "Total significant adverse events"),
    DocumentField("criticalValue", "LLR critical value to define significance"),
    DocumentField("rows", "Significant adverse event entries")
  )

  implicit val otarProjectImp = deriveObjectType[Backend, OtarProject]()
  implicit val otarProjectsImp = deriveObjectType[Backend, OtarProjects]()

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit val geneOntologyImp = deriveObjectType[Backend, GeneOntology](
    ReplaceField("id",
      Field("term",
        OptionType(geneOntologyTermImp),
        Some("Gene ontology term"),
        resolve = r => goFetcher.deferOpt(r.value.id))),
    ReplaceField("ecoId",
      Field("eco",
        OptionType(ecoImp),
        Some("ECO object"),
        resolve = r => ecosFetcher.deferOpt(r.value.evidence)))
  )

  implicit val cancerHallmarkImp = deriveObjectType[Backend, CancerHallmark]()
  implicit val hallmarksAttributeImp = deriveObjectType[Backend, HallmarkAttribute]()
  implicit val hallmarksImp = deriveObjectType[Backend, Hallmarks]()

  //  implicit val orthologImp = deriveObjectType[Backend, Ortholog]()
  //  implicit val orthologsImp = deriveObjectType[Backend, Orthologs]()


  implicit val genotypePhenotypeImp = deriveObjectType[Backend, GenotypePhenotype]()
  implicit val mousePhenotypeImp = deriveObjectType[Backend, MousePhenotype]()
  implicit val mouseGeneImp = deriveObjectType[Backend, MouseGene]()
  implicit val mousePhenotypesImp = deriveObjectType[Backend, MousePhenotypes]()

  implicit val tepImp = deriveObjectType[Backend, Tep](
    ObjectTypeDescription("Target Enabling Package (TEP)")
  )

  implicit val idAndSourceImp = deriveObjectType[Backend, IdAndSource]()
  implicit val locationAndSourceImp = deriveObjectType[Backend, LocationAndSource]()
  implicit val labelAndSourceImp = deriveObjectType[Backend, LabelAndSource]()
  implicit val genomicLocationImp = deriveObjectType[Backend, GenomicLocation]()
  implicit val targetClassImp = deriveObjectType[Backend, TargetClass]()
  implicit val doseTypeImp = deriveObjectType[Backend, DoseAndType]()
  implicit val constraintImp = deriveObjectType[Backend, Constraint]()
  implicit val homologueImp = deriveObjectType[Backend, Homologue]()
  implicit val targetTissueImp = deriveObjectType[Backend, TargetTissue]()
  implicit val safetyLiabilityImp = deriveObjectType[Backend, SafetyLiability]()

  // cancerbiomarkers
  implicit val cancerBiomarkerSourceImp = deriveObjectType[Backend, CancerBiomarkerSource](
    ObjectTypeDescription("Detail on Cancer Biomarker sources"),
    DocumentField("description", "Source description"),
    DocumentField("link", "Source link"),
    DocumentField("name", "Source name")
  )

  implicit val cancerBiomarkerImp = deriveObjectType[Backend, CancerBiomarker](
    ObjectTypeDescription(
      "Entry on clinical relevance and drug responses of tumor genomic " +
        "alterations on the target"),
    DocumentField("id", "Target symbol and variant id"),
    DocumentField("associationType", "Drug responsiveness"),
    DocumentField("drugName", "Drug family or name"),
    DocumentField("evidenceLevel", "Source type"),
    DocumentField("sources", "Sources"),
    DocumentField("pubmedIds", "List of supporting publications"),
    DocumentField("evidenceLevel", "Source type"),
    ReplaceField("target",
                 Field("target",
                       targetImp,
                       Some("Target entity"),
                       resolve = r => targetsFetcher.defer(r.value.target))),
    ReplaceField("disease",
                 Field("disease",
                       OptionType(diseaseImp),
                       Some("Disease entity"),
                       resolve = r => diseasesFetcher.deferOpt(r.value.disease)))
  )

  implicit val cancerBiomarkersImp = deriveObjectType[Backend, CancerBiomarkers](
    ObjectTypeDescription(
      "Set of clinical relevance and drug responses of tumor " +
        "genomic alterations on the target entries"),
    DocumentField("uniqueDrugs", "Number of unique drugs with response information"),
    DocumentField("uniqueDiseases",
                  "Number of unique cancer diseases with drug response information"),
    DocumentField("uniqueBiomarkers", "Number of unique biomarkers with drug response information"),
    DocumentField("count", "Number of entries"),
    DocumentField("rows", "Cancer Biomarker entries")
  )

  // hpo
  implicit lazy val hpoImp: ObjectType[Backend, HPO] = deriveObjectType(
    ObjectTypeDescription("Phenotype entity"),
    DocumentField("id", "Open Targets hpo id"),
    DocumentField("name", "Phenotype name"),
    DocumentField("description", "Phenotype description"),
    DocumentField("namespace", "namespace"),
  )

  // DiseaseHPO
  // More details here: https://hpo.jax.org/app/help/annotations
  implicit val diseaseHPOEvidencesImp = deriveObjectType[Backend, DiseaseHPOEvidences](
    ObjectTypeDescription(
      "the HPO project provides a large set of phenotype annotations. Source: Phenotype.hpoa"),
    DocumentField(
      "aspect",
      "One of P (Phenotypic abnormality), I (inheritance), C (onset and clinical course). Might be null (MONDO)"),
    DocumentField(
      "bioCuration",
      "This refers to the center or user making the annotation and the date on which the annotation was made"),
    DocumentField("diseaseFromSourceId",
                  "This field refers to the database and database identifier. EG. OMIM"),
    DocumentField("diseaseFromSource", "Related name from the field diseaseFromSourceId"),
    ExcludeFields("diseaseName"),
    DocumentField("evidenceType",
                  "This field indicates the level of evidence supporting the annotation."),
    DocumentField("frequency", "A term-id from the HPO-sub-ontology"),
    ReplaceField(
      "modifiers",
      Field("modifiers",
            ListType(hpoImp),
            Some("HP terms from the Clinical modifier subontology"),
            resolve = r => hposFetcher.deferSeqOpt(r.value.modifiers))
    ),
    ReplaceField(
      "onset",
      Field("onset",
            ListType(hpoImp),
            Some("A term-id from the HPO-sub-ontology below the term Age of onset."),
            resolve = r => hposFetcher.deferSeqOpt(r.value.onset))
    ),
    DocumentField(
      "qualifierNot",
      "This optional field can be used to qualify the annotation. Values: [True or False]"),
    DocumentField(
      "references",
      "This field indicates the source of the information used for the annotation (phenotype.hpoa)"),
    DocumentField(
      "sex",
      "This field contains the strings MALE or FEMALE if the annotation in question is limited to males or females."),
    DocumentField("resource", "Possible source mapping: HPO or MONDO"),
    AddFields(
      Field(
        "frequencyHPO",
        OptionType(hpoImp),
        Some("HPO Entity"),
        resolve = r => {
          hposFetcher.deferOpt(r.value.frequency)
        }
      )
    )
  )

  implicit val diseaseHPOImp = deriveObjectType[Backend, DiseaseHPO](
    ObjectTypeDescription("Disease and phenotypes annotations"),
    ReplaceField("phenotype",
                 Field("phenotypeHPO",
                       OptionType(hpoImp),
                       Some("Phenotype entity"),
                       resolve = r => hposFetcher.deferOpt(r.value.phenotype))),
    AddFields(
      Field(
        "phenotypeEFO",
        OptionType(diseaseImp),
        Some("Disease Entity"),
        resolve = r => {
          diseasesFetcher.deferOpt(r.value.phenotype)
        }
      )
    ),
    ExcludeFields("disease"),
    DocumentField("evidence", "List of phenotype annotations.")
  )

  implicit val diseaseHPOsImp = deriveObjectType[Backend, DiseaseHPOs](
    ObjectTypeDescription("List of Phenotypes associated with the disease"),
    DocumentField("count", "Number of entries"),
    DocumentField("rows", "List of Disease and phenotypes annotations")
  )

  // howto doc https://sangria-graphql.org/learn/#macro-based-graphql-type-derivation
  implicit lazy val linkedDiseasesImp = deriveObjectType[Backend, LinkedIds](
    ObjectTypeName("LinkedDiseases"),
    ObjectTypeDescription("Linked Disease Entities"),
    ReplaceField("rows",
                 Field("rows",
                       ListType(diseaseImp),
                       Some("Disease List"),
                       resolve = r => diseasesFetcher.deferSeqOpt(r.value.rows)))
  )

  implicit lazy val linkedTargetsImp = deriveObjectType[Backend, LinkedIds](
    ObjectTypeName("LinkedTargets"),
    ObjectTypeDescription("Linked Target Entities"),
    ReplaceField("rows",
                 Field("rows",
                       ListType(targetImp),
                       Some("Target List"),
                       resolve = r => targetsFetcher.deferSeqOpt(r.value.rows)))
  )

  implicit lazy val drugReferenceImp: ObjectType[Backend, Reference] =
    deriveObjectType[Backend, Reference]()

  implicit lazy val indicationReferenceImp: ObjectType[Backend, IndicationReference] =
    deriveObjectType[Backend, IndicationReference]()

  implicit lazy val mechanismOfActionRowImp: ObjectType[Backend, MechanismOfActionRow] =
    deriveObjectType[Backend, MechanismOfActionRow](
      ReplaceField("targets",
                   Field("targets",
                         ListType(targetImp),
                         Some("Target List"),
                         resolve =
                           r => targetsFetcher.deferSeqOpt(r.value.targets.getOrElse(Seq.empty))))
    )

  implicit lazy val indicationRowImp = deriveObjectType[Backend, IndicationRow](
    ReplaceField("disease",
                 Field("disease",
                       diseaseImp,
                       Some("Disease"),
                       resolve = r => diseasesFetcher.defer(r.value.disease)))
  )

  implicit lazy val indicationsImp = deriveObjectType[Backend, Indications](
    ExcludeFields("id"),
    RenameField("indications", "rows"),
    RenameField("indicationCount", "count"),
  )

  implicit lazy val mechanismOfActionImp = deriveObjectType[Backend, MechanismsOfAction]()

  implicit lazy val drugCrossReferenceImp = deriveObjectType[Backend, DrugReferences]()
  implicit lazy val drugWarningReferenceImp = deriveObjectType[Backend, DrugWarningReference]()

  implicit lazy val drugWarningsImp = deriveObjectType[Backend, DrugWarning](
    ObjectTypeDescription("Drug warnings as calculated by ChEMBL"),
    DocumentField("toxicityClass", "High level toxicity category by Meddra System Organ Class"),
    DocumentField("country", "Country issuing warning"),
    DocumentField("description", "Reason for withdrawal"),
    DocumentField("references", "Source of withdrawal information"),
    DocumentField("warningType", "Either 'black box warning' or 'withdrawn'"),
    DocumentField("year", "Year of withdrawal")
  )

  implicit lazy val drugImp: ObjectType[Backend, Drug] = deriveObjectType[Backend, Drug](
    ObjectTypeDescription("Drug/Molecule entity"),
    DocumentField("id", "Open Targets molecule id"),
    DocumentField("name", "Molecule preferred name"),
    DocumentField("synonyms", "Molecule synonyms"),
    DocumentField("tradeNames", "Drug trade names"),
    DocumentField("yearOfFirstApproval", "Year drug was approved for the first time"),
    DocumentField("drugType", "Drug modality"),
    DocumentField("maximumClinicalTrialPhase",
                  "Maximum phase observed in clinical trial records and" +
                    " post-marketing package inserts"),
    DocumentField("isApproved", "Alias for maximumClinicalTrialPhase == 4"),
    DocumentField("hasBeenWithdrawn", "Has drug been withdrawn from the market"),
    DocumentField("blackBoxWarning", "Alert on life-threteaning drug side effects provided by FDA"),
    DocumentField("description", "Drug description"),
    ReplaceField(
      "parentId",
      Field("parentMolecule",
            OptionType(drugImp),
            description = Some("ChEMBL ID of parent molecule"),
            resolve = r => drugsFetcher.deferOpt(r.value.parentId))
    ),
    ReplaceField(
      "childChemblIds",
      Field(
        "childMolecules",
        ListType(drugImp),
        description = Some("Chembl IDs of molecules that descend from current molecule."),
        resolve = r => drugsFetcher.deferSeqOpt(r.value.childChemblIds.getOrElse(Seq.empty))
      )
    ),
    AddFields(
      Field(
        "approvedIndications",
        OptionType(ListType(StringType)),
        description = Some("Indications for which there is a phase IV clinical trial"),
        resolve = r => DeferredValue(indicationFetcher.deferOpt(r.value.id))
          .map(_.flatMap(_.approvedIndications))
      ),
      Field(
        "drugWarnings",
        ListType(drugWarningsImp),
        description = Some("Warnings present on drug as identified by ChEMBL."),
        resolve = c => {
          c.ctx.getDrugWarnings(c.value.id)
        }
      ),
      Field(
        "similarEntities",
        ListType(similarityGQLImp),
        description = Some("Return similar labels using a model Word2CVec trained with PubMed"),
        arguments = idsArg :: entityNames :: thresholdArg :: pageSize :: Nil,
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
            "alone or in combination with other entities"),
        arguments = idsArg :: cursor :: Nil,
        resolve = c => {
          val ids = c.arg(idsArg).getOrElse(List.empty) ++ List(c.value.id)
          val cur = c.arg(cursor)

          c.ctx.getLiteratureOcurrences(ids.toSet, cur)
        }
      ),
      Field(
        "mechanismsOfAction",
        OptionType(mechanismOfActionImp),
        description = Some(
          "Mechanisms of action to produce intended pharmacological effects. Curated from scientific " +
            "literature and post-marketing package inserts"),
        resolve = ctx => ctx.ctx.getMechanismsOfAction(ctx.value.id)
      ),
      Field(
        "indications",
        OptionType(indicationsImp),
        description = Some(
          "Investigational and approved indications curated from clinical trial records and " +
            "post-marketing package inserts"),
        resolve = ctx => DeferredValue(indicationFetcher.deferOpt(ctx.value.id))
      ),
      Field(
        "knownDrugs",
        OptionType(knownDrugsImp),
        description = Some(
          "Curated Clinical trial records and and post-marketing package inserts " +
            "with a known mechanism of action"),
        arguments = freeTextQuery :: pageSize :: cursor :: Nil,
        resolve = ctx => {
          ctx.ctx.getKnownDrugs(
            ctx.arg(freeTextQuery).getOrElse(""),
            Map("drugId.raw" -> ctx.value.id),
            ctx.arg(pageSize),
            ctx.arg(cursor)
          )
        }
      ),
      Field(
        "adverseEvents",
        OptionType(adverseEventsImp),
        description = Some("Significant adverse events inferred from FAERS reports"),
        arguments = pageArg :: Nil,
        resolve = ctx => ctx.ctx.getAdverseEvents(ctx.value.id, ctx.arg(pageArg))
      )
    ),
    ReplaceField(
      "linkedDiseases",
      Field(
        "linkedDiseases",
        OptionType(linkedDiseasesImp),
        Some(
          "Therapeutic indications for drug based on clinical trial data or " +
            "post-marketed drugs, when mechanism of action is known\""),
        resolve = r => r.value.linkedDiseases
      )
    ),
    ReplaceField(
      "linkedTargets",
      Field(
        "linkedTargets",
        OptionType(linkedTargetsImp),
        Some("Molecule targets based on drug mechanism of action"),
        resolve = ctx => {
          val moa: Future[MechanismsOfAction] = ctx.ctx.getMechanismsOfAction(ctx.value.id)
          val targets: Future[Seq[String]] =
            moa.map(m => m.rows.flatMap(r => r.targets.getOrElse(Seq.empty)))
          targets.map(t => LinkedIds(t.size, t))
        }
      )
    )
  )

  implicit val datasourceSettingsImp = deriveObjectType[Backend, DatasourceSettings]()
  implicit val interactionSettingsImp = deriveObjectType[Backend, LUTableSettings]()
  implicit val associationSettingsImp = deriveObjectType[Backend, AssociationSettings]()
  implicit val targetSettingsImp = deriveObjectType[Backend, TargetSettings]()
  implicit val diseaseSettingsImp = deriveObjectType[Backend, DiseaseSettings]()
  implicit val harmonicSettingsImp = deriveObjectType[Backend, HarmonicSettings]()
  implicit val literatureSettingsImp = deriveObjectType[Backend, LiteratureSettings]()
  implicit val literatureIndexSettingsImp = deriveObjectType[Backend, LiteratureIndexSettings]()
  implicit val clickhouseSettingsImp = deriveObjectType[Backend, ClickhouseSettings]()

  implicit lazy val aggregationImp: ObjectType[Backend, Aggregation] =
    deriveObjectType[Backend, Aggregation]()
  implicit lazy val namedAggregationImp: ObjectType[Backend, NamedAggregation] =
    deriveObjectType[Backend, NamedAggregation]()
  implicit lazy val aggregationsImp: ObjectType[Backend, Aggregations] =
    deriveObjectType[Backend, Aggregations]()
  implicit val evidenceSourceImp = deriveObjectType[Backend, EvidenceSource]()
  implicit val associatedOTFTargetsImp = deriveObjectType[Backend, Associations](
    ObjectTypeName("AssociatedTargets"),
    ReplaceField("rows",
                 Field("rows",
                       ListType(associatedOTFTargetImp),
                       Some("Associated Targets using (On the fly method)"),
                       resolve = r => r.value.rows))
  )

  implicit val associatedOTFDiseasesImp = deriveObjectType[Backend, Associations](
    ObjectTypeName("AssociatedDiseases"),
    ReplaceField("rows",
      Field("rows",
        ListType(associatedOTFDiseaseImp),
        Some("Associated Targets using (On the fly method)"),
        resolve = r => r.value.rows))
  )
  implicit val geneOntologyTermImp = deriveObjectType[Backend, GeneOntologyTerm]()
  implicit val knownDrugReferenceImp = deriveObjectType[Backend, KnownDrugReference]()

  implicit val URLImp: ObjectType[Backend, URL] = deriveObjectType[Backend, URL](
    ObjectTypeDescription("Source URL for clinical trials, FDA and package inserts"),
    DocumentField("url", "resource url"),
    DocumentField("name", "resource name")
  )

  implicit val knownDrugImp: ObjectType[Backend, KnownDrug] = deriveObjectType[Backend, KnownDrug](
    ObjectTypeDescription("Clinical precedence entry for drugs with investigational or " +
      "approved indications targeting gene products according to their curated mechanism of " +
      "action. Entries are grouped by target, disease, drug, phase, status and mechanism of action"),
    DocumentField("approvedSymbol",
                  "Drug target approved symbol based on curated mechanism of action"),
    DocumentField("label", "Curated disease indication"),
    DocumentField("prefName", "Drug name"),
    DocumentField("drugType", "Drug modality"),
    DocumentField("targetId", "Drug target Open Targets id based on curated mechanism of action"),
    DocumentField("diseaseId", "Curated disease indication Open Targets id"),
    DocumentField("drugId", "Open Targets drug id"),
    DocumentField("phase", "Clinical Trial phase"),
    DocumentField("mechanismOfAction", "Mechanism of Action description"),
    DocumentField("status", "Trial status"),
    DocumentField("targetClass", "Drug target class based on curated mechanism of action"),
    DocumentField("references", "Source urls for FDA or package inserts"),
    DocumentField("ctIds", "Clinicaltrials.gov identifiers on entry trials"),
    DocumentField("urls", "Source urls from clinical trials"),
    AddFields(
      Field("disease",
            OptionType(diseaseImp),
            description = Some("Curated disease indication entity"),
            resolve = r => diseasesFetcher.deferOpt(r.value.diseaseId)),
      Field(
        "target",
        OptionType(targetImp),
        description = Some("Drug target entity based on curated mechanism of action"),
        resolve = r => targetsFetcher.deferOpt(r.value.targetId)
      ),
      Field("drug",
            OptionType(drugImp),
            description = Some("Curated drug entity"),
            resolve = r => drugsFetcher.deferOpt(r.value.drugId))
    )
  )

  implicit val knownDrugsImp: ObjectType[Backend, KnownDrugs] =
    deriveObjectType[Backend, KnownDrugs](
      ObjectTypeDescription("Set of clinical precedence for drugs with investigational or " +
        "approved indications targeting gene products according to their curated mechanism of action"),
      DocumentField("uniqueDrugs", "Total unique drugs/molecules"),
      DocumentField("uniqueDiseases", "Total unique diseases or phenotypes"),
      DocumentField("uniqueTargets",
                    "Total unique known mechanism of action targetsTotal " +
                      "unique known mechanism of action targets"),
      DocumentField("count", "Total number of entries"),
      DocumentField("rows", "Clinical precedence entries with known mechanism of action")
    )

  lazy val mUnionType =
    UnionType("EntityUnionType", types = List(targetImp, drugImp, diseaseImp))

  implicit val searchResultAggsCategoryImp =
    deriveObjectType[Backend, models.entities.SearchResultAggCategory]()
  implicit val searchResultAggsEntityImp =
    deriveObjectType[Backend, models.entities.SearchResultAggEntity]()
  implicit val searchResultAggsImp = deriveObjectType[Backend, models.entities.SearchResultAggs]()
  implicit val searchResultImp = deriveObjectType[Backend, models.entities.SearchResult](
    AddFields(
      Field(
        "object",
        OptionType(mUnionType),
        description = Some("Associations for a fixed target"),
        resolve = ctx => {
          ctx.value.entity match {
            case "target"  => targetsFetcher.deferOpt(ctx.value.id)
            case "disease" => diseasesFetcher.deferOpt(ctx.value.id)
            case _         => drugsFetcher.deferOpt(ctx.value.id)
          }
        }
      ))
  )

  implicit val similarityGQLImp = deriveObjectType[Backend, models.entities.Similarity](
    AddFields(
      Field(
        "object",
        OptionType(mUnionType),
        description = Some("Similarity label optionally resolved into an entity"),
        resolve = ctx => {
          ctx.value.category match {
            case "target"  => targetsFetcher.deferOpt(ctx.value.id)
            case "disease" => diseasesFetcher.deferOpt(ctx.value.id)
            case _         => drugsFetcher.deferOpt(ctx.value.id)
          }
        }
      ))
  )

  implicit val searchResultsImp = deriveObjectType[Backend, models.entities.SearchResults]()

  val searchResultsGQLImp = ObjectType(
    "SearchResults",
    "Search results",
    fields[Backend, SearchResults](
      Field("aggregations",
            OptionType(searchResultAggsImp),
            description = Some("Aggregations"),
            resolve = _.value.aggregations),
      Field("hits",
            ListType(searchResultImp),
            description = Some("Return combined"),
            resolve = _.value.hits),
      Field("total",
            LongType,
            description = Some("Total number or results given a entity filter"),
            resolve = _.value.total)
    )
  )
}
