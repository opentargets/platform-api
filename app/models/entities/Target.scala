package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonNaming.SnakeCase

case class Ortholog(speciesId: String,
                    name: String,
                    symbol: String,
                    support: Seq[String],
                    ensemblId: String,
                    dbId: String,
                    entrezId: String,
                    chromosomeId: String,
                    assertIds: Seq[String])

case class Orthologs(chimpanzee: Option[Seq[Ortholog]],
                     dog: Option[Seq[Ortholog]],
                     fly: Option[Seq[Ortholog]],
                     frog: Option[Seq[Ortholog]],
                     macaque: Option[Seq[Ortholog]],
                     mouse: Option[Seq[Ortholog]],
                     pig: Option[Seq[Ortholog]],
                     rat: Option[Seq[Ortholog]],
                     worm: Option[Seq[Ortholog]],
                     yeast: Option[Seq[Ortholog]],
                     zebrafish: Option[Seq[Ortholog]]
                    )

case class LiteratureReference(pubmedId: Long, description: String)

case class CancerHallmark(suppress: Boolean,
                    promote: Boolean,
                    reference: LiteratureReference,
                    label: String)

case class HallmarkAttribute(name: String, reference: LiteratureReference)

case class Hallmarks(rows: Seq[CancerHallmark], attributes: Seq[HallmarkAttribute], functions: Seq[LiteratureReference])

case class ProteinAnnotations(id: String, accessions: Seq[String], functions: Seq[String])

case class GenomicLocation(chromosome: String, start: Long, end: Long, strand: Int)

case class SourceLink(source: String, link: String)

case class PortalProbe(note: String, chemicalprobe: String, gene: String, sourcelinks: Seq[SourceLink])

case class ChemicalProbes(probeminer: Option[String], rows: Seq[PortalProbe])

case class GeneOntology(id: String, project: String, term: String, evidence: String)

case class TractabilityAntibodyCategories(predictedTractableMedLowConfidence: Double,
                                          clinicalPrecedence: Double,
                                          predictedTractableHighConfidence: Double)
case class TractabilitySmallMoleculeCategories(clinicalPrecedence: Double,
                                               predictedTractable: Double,
                                               discoveryPrecedence: Double)
case class TractabilityAntibody(topCategory: String, buckets: Seq[Long], categories: TractabilityAntibodyCategories)
case class TractabilitySmallMolecule(topCategory: String, smallMoleculeGenomeMember: Boolean,
                                     buckets: Seq[Long],
                                     highQualityCompounds: Long,
                                     categories: TractabilitySmallMoleculeCategories
                                    )

case class OtherModalitiesCategories(clinicalPrecedence: Double)
case class OtherModalities(buckets: Seq[Long], categories: OtherModalitiesCategories)
case class Tractability(smallmolecule: Option[TractabilitySmallMolecule], antibody: Option[TractabilityAntibody],
                        otherModalities: Option[OtherModalities])

case class SafetyCode(code: Option[String], mappedTerm: Option[String], termInPaper: Option[String])
case class SafetyReference(pubmedId: Option[Long], refLabel: Option[String], refLink: Option[String])
case class AdverseEffectsActivationEffects(acuteDosing: Seq[SafetyCode], chronicDosing: Seq[SafetyCode],
                                           general: Seq[SafetyCode])
case class AdverseEffectsInhibitionEffects(acuteDosing: Seq[SafetyCode],
                                           chronicDosing: Seq[SafetyCode], general: Seq[SafetyCode],
                                           developmental: Seq[SafetyCode])

case class AdverseEffects(activationEffects: AdverseEffectsActivationEffects,
                          inhibitionEffects: AdverseEffectsInhibitionEffects,
                          organsSystemsAffected: Seq[SafetyCode],
                          references: Seq[SafetyReference])

case class SafetyRiskInfo(organsSystemsAffected: Seq[SafetyCode], references: Seq[SafetyReference],
                          safetyLiability: String)

case class ExperimentDetails(assayFormatType: String, tissue: Option[String], assayFormat: String,
                             assayDescription: String, cellShortName: Option[String])
case class ExperimentalToxicity(dataSource: String, dataSourceReferenceLink: String,
                                experimentDetails: ExperimentDetails)

case class Safety(adverseEffects: Seq[AdverseEffects], safetyRiskInfo: Seq[SafetyRiskInfo],
                  experimentalToxicity: Seq[ExperimentalToxicity])

case class Target(id: String,
                  approvedSymbol: String,
                  approvedName: String,
                  bioType: String,
                  hgncId: Option[String],
                  nameSynonyms: Seq[String],
                  symbolSynonyms: Seq[String],
                  genomicLocation: GenomicLocation,
                  proteinAnnotations: Option[ProteinAnnotations],
                  geneOntology: Seq[GeneOntology],
                  safety: Option[Safety],
                  chemicalProbes: Option[ChemicalProbes],
                  hallmarks: Option[Hallmarks],
                  orthologs: Option[Orthologs],
                  tractability: Option[Tractability]
                 )

object Target {
  val logger = Logger(this.getClass)

  object JSONImplicits {
    implicit val tractabilityAntibodyCategoriesImpW = Json.writes[TractabilityAntibodyCategories]
    implicit val tractabilityAntibodyCategoriesImpR: Reads[TractabilityAntibodyCategories] =
      ((__ \ "predicted_tractable_med_low_confidence").read[Double] and
        (__ \ "clinical_precedence").read[Double] and
        (__ \ "predicted_tractable_high_confidence").read[Double]
      )(TractabilityAntibodyCategories.apply _)

    implicit val tractabilitySmallMoleculeCategoriesImpW = Json.writes[TractabilitySmallMoleculeCategories]
    implicit val tractabilitySmallMoleculeCategoriesImpR: Reads[TractabilitySmallMoleculeCategories] =
      ((__ \ "clinical_precedence").read[Double] and
        (__ \ "predicted_tractable").read[Double] and
        (__ \ "discovery_precedence").read[Double]
        )(TractabilitySmallMoleculeCategories.apply _)

    implicit val tractabilityAntibodyImpW = Json.writes[TractabilityAntibody]
    implicit val TractabilityAntibodyImpR: Reads[TractabilityAntibody] =
      ((__ \ "top_category").read[String] and
        (__ \ "buckets").read[Seq[Long]] and
        (__ \ "categories").read[TractabilityAntibodyCategories]
        )(TractabilityAntibody.apply _)

    implicit val tractabilitySmallMoleculeImpW = Json.writes[TractabilitySmallMolecule]
    implicit val tractabilitySmallMoleculeImpR: Reads[TractabilitySmallMolecule] =
      ((__ \ "top_category").read[String] and
        (__ \ "small_molecule_genome_member").read[Boolean] and
        (__ \ "buckets").read[Seq[Long]] and
        (__ \ "high_quality_compounds").read[Long] and
        (__ \ "categories").read[TractabilitySmallMoleculeCategories]
        )(TractabilitySmallMolecule.apply _)

    implicit val otherModalitiesCategoriesImpW = Json.writes[OtherModalitiesCategories]

    implicit val otherModalitiesImpW = Json.writes[OtherModalities]
    implicit val otherModalitiesImpR: Reads[OtherModalities] = (
      (__ \ "buckets").read[Seq[Long]] and
        (__ \ "categories" \ "clinical_precedence").read[Double].map(OtherModalitiesCategories)
    )(OtherModalities)

    implicit val tractabilityImpW = Json.writes[Tractability]
    implicit val tractabilityImpR: Reads[Tractability] =
      ((__ \ "smallmolecule").readNullable[TractabilitySmallMolecule] and
        (__ \ "antibody").readNullable[TractabilityAntibody] and
        (__ \ "other_modalities").readNullable[OtherModalities]
        )(Tractability.apply _)

    implicit val orthologImpW = Json.writes[Ortholog]
    implicit val orthologImpR: Reads[Ortholog] =
      ((__ \ "ortholog_species").read[String] and
        (__ \ "ortholog_species_name").read[String] and
        (__ \ "ortholog_species_symbol").read[String] and
        (__ \ "support").read[Seq[String]] and
        (__ \ "ortholog_species_ensembl_gene").read[String] and
        (__ \ "ortholog_species_db_id").read[String] and
        (__ \ "ortholog_species_entrez_gene").read[String] and
        (__ \ "ortholog_species_chr").read[String] and
        (__ \ "ortholog_species_assert_ids").read[Seq[String]]
      )(Ortholog.apply _)

    implicit val orthologsImpF = Json.format[Orthologs]

    implicit val literatureReferenceImpW = Json.writes[LiteratureReference]
    implicit val literatureReferenceImpR: Reads[LiteratureReference] =
      ((__ \ "pmid").read[Long] and
        (__ \ "description").read[String]
        )(LiteratureReference.apply _)

    implicit val cancerHallmarkImpW = Json.writes[CancerHallmark]
    implicit val cancerHallmarkImpR: Reads[CancerHallmark] =
      ((__ \ "suppress").read[Boolean] and
        (__ \ "promote").read[Boolean] and
        literatureReferenceImpR and
        (__ \ "label").read[String]
        )(CancerHallmark.apply _)

    implicit val hallmarkAttributeImpW = Json.writes[HallmarkAttribute]
    implicit val hallmarkAttributeImpR: Reads[HallmarkAttribute] =
      ((__ \ "attribute_name").read[String] and
        literatureReferenceImpR
        )(HallmarkAttribute.apply _)

    implicit val hallmarksImpW = Json.writes[Hallmarks]
    implicit val hallmarksImpR: Reads[Hallmarks] =
      ((__ \ "cancer_hallmarks").readWithDefault[Seq[CancerHallmark]](Seq.empty) and
      (__ \ "attributes").readWithDefault[Seq[HallmarkAttribute]](Seq.empty) and
        (__ \ "function_summary").readWithDefault[Seq[LiteratureReference]](Seq.empty)
        )(Hallmarks.apply _)

    implicit val sourceLinkImpF = Json.format[models.entities.SourceLink]
    implicit val portalProbeImpF = Json.format[models.entities.PortalProbe]
    implicit val chemicalProbesImpW = Json.writes[models.entities.ChemicalProbes]
    implicit val chemicalProbesImpR: Reads[models.entities.ChemicalProbes] =
      ((__ \ "probeminer" \ "link").readNullable[String] and
        (__ \ "portalprobes").readWithDefault[Seq[PortalProbe]](Seq.empty)
        )(ChemicalProbes.apply _)

    implicit val safetyCodeImpW = Json.writes[models.entities.SafetyCode]
    implicit val safetyCodeImpR: Reads[models.entities.SafetyCode] =
    ((__ \ "mapped_term").readNullable[String].map {
      case Some("") => None
      case x => x
    } and
      (__ \ "term_in_paper").readNullable[String].map {
        case Some("") => None
        case x => x
      } and
      (__ \ "code").readNullable[String].map {
        case Some("") => None
        case x => x
      }
      )(SafetyCode.apply _)

    implicit val safetyReferenceImpW = Json.writes[models.entities.SafetyReference]
    implicit val safetyReferenceImpR: Reads[models.entities.SafetyReference] =
      ((__ \ "pmid").readNullable[Long] and
        (__ \ "ref_label").readNullable[String].map {
          case Some("") => None
          case x => x
        } and
        (__ \ "ref_link").readNullable[String].map {
          case Some("") => None
          case x => x
        }
        )(SafetyReference.apply _)

    implicit val adverseEffectsActivationEffectsImpW = Json.writes[AdverseEffectsActivationEffects]
    implicit val adverseEffectsActivationEffectsImpR: Reads[models.entities.AdverseEffectsActivationEffects] =
      ((__ \ "acute_dosing").readWithDefault[Seq[SafetyCode]](Seq.empty) and
        (__ \ "chronic_dosing").readWithDefault[Seq[SafetyCode]](Seq.empty) and
        (__ \ "general").readWithDefault[Seq[SafetyCode]](Seq.empty)
        )(AdverseEffectsActivationEffects.apply _)

    implicit val adverseEffectsInhibitionEffectsImpW = Json.writes[AdverseEffectsInhibitionEffects]
    implicit val adverseEffectsInhibitionEffectsImpR: Reads[models.entities.AdverseEffectsInhibitionEffects] =
      ((__ \ "acute_dosing").readWithDefault[Seq[SafetyCode]](Seq.empty) and
        (__ \ "chronic_dosing").readWithDefault[Seq[SafetyCode]](Seq.empty) and
        (__ \ "general").readWithDefault[Seq[SafetyCode]](Seq.empty) and
        (__ \ "developmental").readWithDefault[Seq[SafetyCode]](Seq.empty)
        )(AdverseEffectsInhibitionEffects.apply _)

    implicit val adverseEffectsImpW = Json.writes[models.entities.AdverseEffects]
    implicit val adverseEffectsImpR: Reads[models.entities.AdverseEffects] =
      ((__ \ "activation_effects").read[AdverseEffectsActivationEffects] and
        (__ \ "inhibition_effects").read[AdverseEffectsInhibitionEffects] and
        (__ \ "organs_systems_affected").read[Seq[SafetyCode]] and
        (__ \ "references").read[Seq[SafetyReference]]
        )(AdverseEffects.apply _)

    implicit val safetyRiskInfoImpW = Json.writes[models.entities.SafetyRiskInfo]
    implicit val safetyRiskInfoImpR: Reads[models.entities.SafetyRiskInfo] =
    ((__ \ "organs_systems_affected").read[Seq[SafetyCode]] and
      (__ \ "references").read[Seq[SafetyReference]] and
      (__ \ "safety_liability").read[String]
      )(SafetyRiskInfo.apply _)

    implicit val experimentalDetailsImpW = Json.writes[ExperimentDetails]
    implicit val experimentalDetailsImpR: Reads[ExperimentDetails] =
      ((__ \ "assay_format_type").read[String] and
        (__ \ "tissue").readNullable[String] and
        (__ \ "assay_format").read[String] and
        (__ \ "assay_description").read[String] and
        (__ \ "cell_short_name").readNullable[String]
        )(ExperimentDetails.apply _)


    implicit val experimentalToxicityImpW = Json.writes[ExperimentalToxicity]
    implicit val experimentalToxicityImpR: Reads[ExperimentalToxicity] =
    ((__ \ "data_source").read[String] and
      (__ \ "data_source_reference_link").read[String] and
      (__ \ "experiment_details").read[ExperimentDetails]
      )(ExperimentalToxicity.apply _)


    implicit val safetyImpW = Json.writes[models.entities.Safety]
    implicit val safetyImpR: Reads[models.entities.Safety] =
      ((__ \ "adverse_effects").readWithDefault[Seq[AdverseEffects]](Seq.empty) and
        (__ \ "safety_risk_info").readWithDefault[Seq[SafetyRiskInfo]](Seq.empty) and
        (__ \ "experimental_toxicity").readWithDefault[Seq[ExperimentalToxicity]](Seq.empty)
        )(Safety.apply _)

    implicit val geneOntologyImpW = Json.writes[models.entities.GeneOntology]
    implicit val geneOntologyImpR: Reads[models.entities.GeneOntology] =
      ((__ \ "id").read[String] and
        (__ \ "value" \ "project").read[String] and
        (__ \ "value" \ "term").read[String] and
        (__ \ "value" \ "evidence").read[String]
        )(GeneOntology.apply _)

    implicit val proteinImpW = Json.format[models.entities.ProteinAnnotations]
    implicit val genomicLocationImpW = Json.format[models.entities.GenomicLocation]
    implicit val targetImpW = Json.writes[models.entities.Target]
    implicit val targetImpR: Reads[models.entities.Target] = (
      (JsPath \ "id").read[String] and
      (JsPath \ "approvedSymbol").read[String] and
      (JsPath \ "approvedName").read[String] and
      (JsPath \ "bioType").read[String] and
      (JsPath \ "hgncId").readNullable[String] and
      (JsPath \ "nameSynonyms").read[Seq[String]] and
      (JsPath \ "symbolSynonyms").read[Seq[String]] and
      (JsPath \ "genomicLocation").read[GenomicLocation] and
      (JsPath \ "proteinAnnotations").readNullable[ProteinAnnotations] and
      (JsPath \ "go").readWithDefault[Seq[GeneOntology]](Seq.empty) and
        (JsPath \ "safety").readNullable[Safety] and
        (JsPath \ "chemicalProbes").readNullable[ChemicalProbes] and
        (JsPath \ "hallMarks").readNullable[Hallmarks] and
        (JsPath \ "ortholog").readNullable[Orthologs] and
        (JsPath \ "tractability").readNullable[Tractability]
      )(Target.apply _)
  }

  def fromJsValue(jObj: JsValue): Option[Target] = {
    /* apply transformers for json and fill the target
     start from internal objects and then map the external
     */
    import Target.JSONImplicits._
    val source = (__ \ '_source).json.pick
    jObj.transform(source).asOpt.map(obj => {
      logger.debug(Json.prettyPrint(obj))
      obj.as[Target]
    })
  }
}
