package models.entities

import play.api.Logging
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class ChemicalProbeUrl(niceName: String, url: Option[String])

case class ChemicalProbe(
    id: String,
    control: Option[String],
    drugId: Option[String],
    mechanismOfAction: Option[Seq[String]],
    isHighQuality: Boolean,
    origin: Option[Seq[String]],
    probeMinerScore: Option[Double],
    probesDrugsScore: Option[Double],
    scoreInCells: Option[Double],
    scoreInOrganisms: Option[Double],
    targetFromSourceId: String,
    urls: Vector[ChemicalProbeUrl]
)

case class SafetyEffects(
    direction: String,
    dosing: Option[String]
)

case class LocationAndSource(
    location: String,
    source: String,
    termSL: Option[String],
    labelSL: Option[String]
)

case class TargetClass(id: Long, label: String, level: String)

case class LabelAndSource(label: String, source: String)

case class Tractability(id: String, modality: String, value: Boolean)

case class SafetyBiosample(
    tissueLabel: Option[String],
    tissueId: Option[String],
    cellLabel: Option[String],
    cellFormat: Option[String],
    cellId: Option[String]
)

case class SafetyStudy(name: Option[String], description: Option[String], `type`: Option[String])

case class SafetyLiability(
    biosamples: Option[Seq[SafetyBiosample]],
    datasource: String,
    effects: Option[Seq[SafetyEffects]],
    event: Option[String],
    eventId: Option[String],
    literature: Option[String],
    url: Option[String],
    studies: Option[Seq[SafetyStudy]]
)

case class CancerHallmark(description: String, impact: Option[String], label: String, pmid: Long)

case class HallmarkAttribute(name: String, description: String, pmid: Option[Long])

case class Hallmarks(cancerHallmarks: Seq[CancerHallmark], attributes: Seq[HallmarkAttribute])

case class Homologue(
    homologyType: String,
    queryPercentageIdentity: Double,
    speciesId: String,
    speciesName: String,
    targetGeneId: String,
    targetGeneSymbol: String,
    targetPercentageIdentity: Double,
    isHighConfidence: Option[String]
)

case class GenomicLocation(chromosome: String, start: Long, end: Long, strand: Int)

case class GeneOntology(
    id: String,
    aspect: String,
    evidence: String,
    geneProduct: String,
    source: String
)

case class GeneOntologyLookup(id: String, name: String)

case class Tep(
    targetFromSourceId: String,
    url: String,
    therapeuticArea: String,
    description: String
)

case class IdAndSource(id: String, source: String)

case class Constraint(
    constraintType: String,
    exp: Option[Double],
    obs: Option[Long],
    oe: Option[Double],
    oeLower: Option[Double],
    oeUpper: Option[Double],
    score: Option[Double],
    upperBin: Option[Long],
    upperBin6: Option[Long],
    upperRank: Option[Long]
)

case class ReactomePathway(pathway: String, pathwayId: String, topLevelTerm: String)

case class Target(
    id: String,
    alternativeGenes: Seq[String],
    approvedSymbol: String,
    approvedName: String,
    biotype: String,
    chemicalProbes: Seq[ChemicalProbe],
    dbXrefs: Seq[IdAndSource],
    functionDescriptions: Seq[String],
    geneticConstraint: Seq[Constraint],
    genomicLocation: GenomicLocation,
    geneOntology: Seq[GeneOntology],
    hallmarks: Option[Hallmarks],
    homologues: Seq[Homologue],
    pathways: Seq[ReactomePathway],
    proteinIds: Seq[IdAndSource],
    safetyLiabilities: Seq[SafetyLiability],
    subcellularLocations: Seq[LocationAndSource],
    synonyms: Seq[LabelAndSource], // double check, this is name and symbol
    symbolSynonyms: Seq[LabelAndSource],
    nameSynonyms: Seq[LabelAndSource],
    obsoleteSymbols: Seq[LabelAndSource],
    obsoleteNames: Seq[LabelAndSource],
    targetClass: Seq[TargetClass],
    tep: Option[Tep],
    tractability: Seq[Tractability],
    transcriptIds: Seq[String]
)

object Target extends Logging {

  implicit val cancerHallmarkImpW: OWrites[CancerHallmark] = Json.writes[CancerHallmark]
  implicit val cancerHallmarkImpR: Reads[CancerHallmark] =
    ((__ \ "description").read[String] and
      (__ \ "impact").readNullable[String] and
      (__ \ "label").read[String] and
      (__ \ "pmid").read[Long])(CancerHallmark.apply)

  implicit val chemicalProbeUrlImp: OFormat[ChemicalProbeUrl] = Json.format[ChemicalProbeUrl]
  implicit val chemicalProbeImp: OFormat[ChemicalProbe] = Json.format[ChemicalProbe]

  implicit val hallmarkAttributeImpW: OWrites[HallmarkAttribute] = Json.writes[HallmarkAttribute]
  implicit val hallmarkAttributeImpR: Reads[HallmarkAttribute] =
    ((__ \ "attribute_name").read[String] and
      (__ \ "description").read[String] and
      (__ \ "pmid").readNullable[Long])(HallmarkAttribute.apply)

  implicit val hallmarksImpW: OWrites[Hallmarks] = Json.writes[Hallmarks]
  implicit val hallmarksImpR: Reads[Hallmarks] =
    ((__ \ "cancerHallmarks").readWithDefault[Seq[CancerHallmark]](Seq.empty) and
      (__ \ "attributes").readWithDefault[Seq[HallmarkAttribute]](Seq.empty))(Hallmarks.apply)

  implicit val geneOntologyImpW: OWrites[GeneOntology] = Json.writes[models.entities.GeneOntology]
  implicit val geneOntologyImpR: Reads[models.entities.GeneOntology] =
    ((__ \ "id").read[String] and
      (__ \ "aspect").read[String] and
      (__ \ "evidence").read[String] and
      (__ \ "geneProduct").read[String] and
      (__ \ "source").read[String])(GeneOntology.apply)

  implicit val tepImpF: OFormat[Tep] = Json.format[Tep]
  implicit val idAndSourceImpF: OFormat[IdAndSource] = Json.format[IdAndSource]
  implicit val labelAndSourceImpF: OFormat[LabelAndSource] = Json.format[LabelAndSource]
  implicit val locationAndSourceImpF: OFormat[LocationAndSource] = Json.format[LocationAndSource]
  implicit val targetClassImpF: OFormat[TargetClass] = Json.format[TargetClass]
  implicit val tractabilityImpF: OFormat[Tractability] = Json.format[Tractability]
  implicit val constraintImpF: OFormat[Constraint] = Json.format[Constraint]
  implicit val homologueImpF: OFormat[Homologue] = Json.format[Homologue]
  implicit val doseAndTypeImpF: OFormat[SafetyEffects] = Json.format[SafetyEffects]
  implicit val tissueImpF: OFormat[SafetyBiosample] = Json.format[SafetyBiosample]
  implicit val safetyStudyImpF: OFormat[SafetyStudy] = Json.format[SafetyStudy]
  implicit val safetyLiabilityImpF: OFormat[SafetyLiability] = Json.format[SafetyLiability]
  implicit val genomicLocationImpW: OFormat[GenomicLocation] =
    Json.format[models.entities.GenomicLocation]
  implicit val reactomePathwayImpF: OFormat[ReactomePathway] =
    Json.format[models.entities.ReactomePathway]
  implicit val targetImpF: OFormat[Target] =
    Json.format[models.entities.Target]
}
