package models.entities

import play.api.libs.functional.syntax.*
import play.api.libs.json.Reads.*
import play.api.libs.json.*
import clickhouse.rep.SeqRep.*
import slick.jdbc.GetResult
import utils.OTLogging

case class CanonicalTranscript(
    id: String,
    chromosome: String,
    start: Long,
    end: Long,
    strand: String
)

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
    alternativeGenes: Seq[String] = Seq.empty,
    approvedName: String,
    approvedSymbol: String,
    biotype: String,
    canonicalTranscript: Option[CanonicalTranscript] = None,
    chemicalProbes: Seq[ChemicalProbe] = Seq.empty,
    constraint: Seq[Constraint], // = Seq.empty,
    dbXrefs: Seq[IdAndSource] = Seq.empty,
    functionDescriptions: Seq[String] = Seq.empty,
    genomicLocation: GenomicLocation,
    go: Seq[GeneOntology] = Seq.empty,
    hallmarks: Option[Hallmarks],
    homologues: Seq[Homologue] = Seq.empty,
    nameSynonyms: Seq[LabelAndSource] = Seq.empty,
    obsoleteNames: Seq[LabelAndSource] = Seq.empty,
    obsoleteSymbols: Seq[LabelAndSource] = Seq.empty,
    pathways: Seq[ReactomePathway] = Seq.empty,
    proteinIds: Seq[IdAndSource] = Seq.empty,
    safetyLiabilities: Seq[SafetyLiability] = Seq.empty,
    studyLocusIds: Seq[String] = Seq.empty,
    subcellularLocations: Seq[LocationAndSource] = Seq.empty,
    symbolSynonyms: Seq[LabelAndSource] = Seq.empty,
    synonyms: Seq[LabelAndSource] = Seq.empty, // double check, this is name and symbol
    targetClass: Seq[TargetClass] = Seq.empty,
    tep: Option[Tep],
    tractability: Seq[Tractability] = Seq.empty,
    transcriptIds: Seq[String] = Seq.empty
)

object Target extends OTLogging {

  implicit val getTargetFromDB: GetResult[Target] =
    GetResult(r => Json.parse(r.<<[String]).as[Target])

  implicit val tepImpW: OWrites[Tep] = Json.writes[Tep]
  implicit val tepImpR: Reads[Tep] =
    (
      (__ \ "targetFromSourceId").read[String] and
        (__ \ "url").read[String] and
        (__ \ "therapeuticArea").read[String] and
        (__ \ "description").read[String]
    )((tar, url, t, desc) =>
      (tar, url, t, desc) match {
        case ("", "", "", "")    => null
        case (tar, url, t, desc) => Tep(tar, url, t, desc)
      }
    )
  implicit val idAndSourceImpF: OFormat[IdAndSource] = Json.format[IdAndSource]
  implicit val labelAndSourceImpF: OFormat[LabelAndSource] = Json.format[LabelAndSource]
  implicit val locationAndSourceImpF: OFormat[LocationAndSource] = Json.format[LocationAndSource]
  implicit val targetClassImpF: OFormat[TargetClass] = Json.format[TargetClass]
  implicit val tractabilityImpF: OFormat[Tractability] = Json.format[Tractability]
  implicit val canonicalTranscriptImpF: OFormat[CanonicalTranscript] =
    Json.format[CanonicalTranscript]
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
  implicit val chemicalProbeF: OFormat[ChemicalProbe] =
    Json.format[models.entities.ChemicalProbe]
  implicit val chemicalProbeUrlF: OFormat[ChemicalProbeUrl] =
    Json.format[models.entities.ChemicalProbeUrl]
  implicit val geneOntologyImpF: OFormat[GeneOntology] = Json.format[models.entities.GeneOntology]
  implicit val geneOntologyLookupImpF: OFormat[GeneOntologyLookup] =
    Json.format[models.entities.GeneOntologyLookup]
  implicit val cancerHallmarkImpF: OFormat[CancerHallmark] =
    Json.format[models.entities.CancerHallmark]
  implicit val hallmarkAttributeImpF: OFormat[HallmarkAttribute] =
    Json.format[models.entities.HallmarkAttribute]
  implicit val hallmarksImpF: OFormat[Hallmarks] = Json.format[models.entities.Hallmarks]
  implicit val targetImpF: OFormat[Target] =
    Json.format[Target]
}
