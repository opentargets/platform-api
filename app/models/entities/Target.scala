package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

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

case class LiteratureReference(pubmedId: String, description: String)

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
                                     ensemble: Double,
                                     categories: TractabilitySmallMoleculeCategories
                                    )
case class Tractability(smallmolecule: Option[TractabilitySmallMolecule], antibody: Option[TractabilityAntibody])

case class SafetyCode(code: String, mappedTerm: String, termInPaper: String)
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
case class Safety(adverseEffects: Seq[AdverseEffects], safetyRiskInfo: Seq[SafetyRiskInfo])

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
                  orthologs: Option[Orthologs]
                 )

object Target {
  val logger = Logger(this.getClass)

  object JSONImplicits {

    // orthologs
    //              {
    //                "ortholog_species" : "9544",
    //                "ortholog_species_name" : "B-Raf proto-oncogene, serine/threonine kinase",
    //                "ortholog_species_symbol" : "BRAF",
    //                "support" : [
    //                  "Ensembl",
    //                  "HomoloGene",
    //                  "NCBI",
    //                  "OrthoDB"
    //                ],
    //                "ortholog_species_ensembl_gene" : "ENSMMUG00000042793",
    //                "ortholog_species_db_id" : "-",
    //                "ortholog_species_entrez_gene" : "693554",
    //                "ortholog_species_chr" : "3",
    //                "ortholog_species_assert_ids" : [
    //                  "ENSMMUG00000042793",
    //                  "3197",
    //                  "693554",
    //                  "11271at9443"
    //                ]
    //case class Ortholog(speciesId: String,
    //                    name: String,
    //                    symbol: String,
    //                    support: Seq[String],
    //                    ensemblId: String,
    //                    dbId: String,
    //                    entrezId: String,
    //                    chromosomeId: String,
    //                    assertIds: Seq[String])

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
      ((__ \ "pmid").read[String] and
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
      ((__ \ "cancer_hallmarks").readNullable[Seq[CancerHallmark]].map(_.getOrElse(Seq.empty)) and
        (__ \ "attributes").readNullable[Seq[HallmarkAttribute]].map(_.getOrElse(Seq.empty)) and
        (__ \ "function_summary").readNullable[Seq[LiteratureReference]].map(_.getOrElse(Seq.empty))
        )(Hallmarks.apply _)

    implicit val sourceLinkImpF = Json.format[models.entities.SourceLink]
    implicit val portalProbeImpF = Json.format[models.entities.PortalProbe]
    implicit val chemicalProbesImpW = Json.writes[models.entities.ChemicalProbes]
    implicit val chemicalProbesImpR: Reads[models.entities.ChemicalProbes] =
      ((__ \ "probeminer" \ "link").readNullable[String] and
        (__ \ "portalprobes").readNullable[Seq[PortalProbe]].map(_.getOrElse(Seq.empty))
        )(ChemicalProbes.apply _)

    implicit val safetyCodeImpW = Json.writes[models.entities.SafetyCode]
    implicit val safetyCodeImpR: Reads[models.entities.SafetyCode] =
    ((__ \ "mapped_term").read[String] and
      (__ \ "term_in_paper").read[String] and
      (__ \ "code").read[String]
      )(SafetyCode.apply _)

    implicit val safetyReferenceImpW = Json.writes[models.entities.SafetyReference]
    implicit val safetyReferenceImpR: Reads[models.entities.SafetyReference] =
      ((__ \ "pmid").readNullable[String].map{
        case Some(pid) =>
          if (pid.isEmpty) None
          else Some(pid.toLong)
        case None => None
      } and
        (__ \ "ref_label").readNullable[String].map {
          case Some(label) =>
            if (label.isEmpty) None
            else Some(label)
          case None => None
        } and
        (__ \ "ref_link").readNullable[String].map {
          case Some(link) =>
            if (link.isEmpty) None
            else Some(link)
          case None => None
        }
        )(SafetyReference.apply _)

    implicit val adverseEffectsActivationEffectsImpW = Json.writes[AdverseEffectsActivationEffects]
    implicit val adverseEffectsActivationEffectsImpR: Reads[models.entities.AdverseEffectsActivationEffects] =
      ((__ \ "acute_dosing").readNullable[Seq[SafetyCode]].map(_.getOrElse(Seq.empty)) and
        (__ \ "chronic_dosing").readNullable[Seq[SafetyCode]].map(_.getOrElse(Seq.empty)) and
        (__ \ "general").readNullable[Seq[SafetyCode]].map(_.getOrElse(Seq.empty))
        )(AdverseEffectsActivationEffects.apply _)

    implicit val adverseEffectsInhibitionEffectsImpW = Json.writes[AdverseEffectsInhibitionEffects]
    implicit val adverseEffectsInhibitionEffectsImpR: Reads[models.entities.AdverseEffectsInhibitionEffects] =
      ((__ \ "acute_dosing").readNullable[Seq[SafetyCode]].map(_.getOrElse(Seq.empty)) and
        (__ \ "chronic_dosing").readNullable[Seq[SafetyCode]].map(_.getOrElse(Seq.empty)) and
        (__ \ "general").readNullable[Seq[SafetyCode]].map(_.getOrElse(Seq.empty)) and
        (__ \ "developmental").readNullable[Seq[SafetyCode]].map(_.getOrElse(Seq.empty))
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

    implicit val safetyImpW = Json.writes[models.entities.Safety]
    implicit val safetyImpR: Reads[models.entities.Safety] =
      ((__ \ "adverse_effects").readNullable[Seq[AdverseEffects]].map(_.getOrElse(Seq.empty)) and
        (__ \ "safety_risk_info").readNullable[Seq[SafetyRiskInfo]].map(_.getOrElse(Seq.empty))
        )(Safety.apply _)

    implicit val geneOntologyImpW = Json.writes[models.entities.GeneOntology]
    implicit val geneOntologyImpR: Reads[models.entities.GeneOntology] =
      ((__ \ "id").read[String] and
        (__ \ "value" \ "project").read[String] and
        (__ \ "value" \ "term").read[String] and
        (__ \ "value" \ "evidence").read[String].map(_.replace(":", "_"))
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
      (JsPath \ "go").readNullable[Seq[GeneOntology]].map{
        case None => Seq.empty
        case Some(s) => s
      } and
        (JsPath \ "safety").readNullable[Safety] and
        (JsPath \ "chemicalProbes").readNullable[ChemicalProbes] and
        (JsPath \ "hallMarks").readNullable[Hallmarks] and
        (JsPath \ "ortholog").readNullable[Orthologs]
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
