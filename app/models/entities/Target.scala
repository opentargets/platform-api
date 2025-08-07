package models.entities

import clickhouse.rep.SeqRep._
import play.api.Logging
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import slick.jdbc.GetResult
import models.gql.Arguments.studyIds
import org.checkerframework.checker.units.qual.g
import org.checkerframework.checker.units.qual.t

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
    chemicalProbes: Seq[ChemicalProbe] = Seq.empty,
    constraint: Seq[Constraint] = Seq.empty,
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
    studyIds: Seq[String] = Seq.empty,
    subcellularLocations: Seq[LocationAndSource] = Seq.empty,
    symbolSynonyms: Seq[LabelAndSource] = Seq.empty,
    synonyms: Seq[LabelAndSource] = Seq.empty, // double check, this is name and symbol
    targetClass: Seq[TargetClass] = Seq.empty,
    tep: Option[Tep],
    tractability: Seq[Tractability] = Seq.empty,
    transcriptIds: Seq[String] = Seq.empty
)

object Target extends Logging {

  implicit val getTargetFromDB: GetResult[Target] =
    GetResult(r => Json.parse(r.<<[String]).as[Target])
  // val id: String = r.<<
  // val alternativeGenes: String = r.<<
  // val approvedName: String = r.<<
  // val approvedSymbol: String = r.<<
  // val biotype: String = r.<<
  // val chemicalProbes: String = r.<<
  // val constraint: String = r.<<
  // val dbXrefs: String = r.<<
  // val functionDescriptions: String = r.<<
  // val genomicLocation: String = r.<<
  // val go: String = r.<<
  // val hallmarks: String = r.<<?
  // val homologues: String = r.<<
  // val nameSynonyms: String = r.<<
  // val obsoleteNames: String = r.<<
  // val obsoleteSymbols: String = r.<<
  // val pathways: String = r.<<
  // val proteinIds: String = r.<<
  // val safetyLiabilities: String = r.<<
  // val studyIds: String = r.<<
  // val subcellularLocations: String = r.<<
  // val symbolSynonyms: String = r.<<
  // val synonyms: String = r.<<
  // val targetClass: String = r.<<
  // val tep: String = r.<<?
  // val tractability: String = r.<<
  // val transcriptIds: String = r.<<
  //     Target(
  //       id = r.<<[String],
  //       alternativeGenes = StrSeqRep(r.<<[String]).rep,
  //       approvedName = r.<<[String],
  //       approvedSymbol = r.<<[String],
  //       biotype = r.<<[String],
  //       chemicalProbes = TupleSeqRep[ChemicalProbe](r.<<[String], chemicalProbeParser).rep,
  //       constraint = TupleSeqRep[Constraint](r.<<[String], constraintParser).rep,
  //       dbXrefs = r.nextObject().asInstanceOf[Seq[IdAndSource]],
  //       functionDescriptions =
  //     )
  //   }

  // class ChemicalProbeParser(str: String) extends StringParser[ChemicalProbe](str) {
  //   override def parse: ChemicalProbe =
  //     ChemicalProbe(
  //       id = tokens(0),
  //       control = optionalValue(tokens(1))(identity),
  //       drugId = optionalValue(tokens(2))(identity),
  //       mechanismOfAction = optionalValue(tokens(3))(s => StrSeqRep(s).rep),
  //       isHighQuality = tokens(4).toBoolean,
  //       origin = optionalValue(tokens(5))(s => StrSeqRep(s).rep),
  //       probeMinerScore = optionalValue(tokens(6))(_.toDouble),
  //       probesDrugsScore = optionalValue(tokens(7))(_.toDouble),
  //       scoreInCells = optionalValue(tokens(8))(_.toDouble),
  //       scoreInOrganisms = optionalValue(tokens(9))(_.toDouble),
  //       targetFromSourceId = tokens(10),
  //       urls = TupleSeqRep(tokens(11), chemicalProbeUrlParser).rep
  //     )
  // }

  // class ChemicalProbeUrlParser(str: String) extends StringParser[ChemicalProbeUrl](str) {
  //   override def parse: ChemicalProbeUrl =
  //     ChemicalProbeUrl(
  //       niceName = tokens(0),
  //       url = optionalValue(tokens(1))(identity)
  //     )
  // }

  // class ConstraintParser(str: String) extends StringParser[Constraint](str) {
  //   override def parse: Constraint =
  //     Constraint(
  //       constraintType = tokens(0),
  //       exp = optionalValue(tokens(1))(_.toDouble),
  //       obs = optionalValue(tokens(2))(_.toLong),
  //       oe = optionalValue(tokens(3))(_.toDouble),
  //       oeLower = optionalValue(tokens(4))(_.toDouble),
  //       oeUpper = optionalValue(tokens(5))(_.toDouble),
  //       score = optionalValue(tokens(6))(_.toDouble),
  //       upperBin = optionalValue(tokens(7))(_.toLong),
  //       upperBin6 = optionalValue(tokens(8))(_.toLong),
  //       upperRank = optionalValue(tokens(9))(_.toLong)
  //     )
  // }
  // // Parser functions
  // val chemicalProbeParser: String => ChemicalProbe = str => new ChemicalProbeParser(str).parse
  // val chemicalProbeUrlParser: String => ChemicalProbeUrl = str =>
  //   new ChemicalProbeUrlParser(str).parse
  // val constraintParser: String => Constraint = str => new ConstraintParser(str).parse

  // val chemicalProbeParser: String => ChemicalProbe = str => {
  //   val tokens = str.split(",")
  //   ChemicalProbe(
  //     id = tokens(0),
  //     control = if (tokens(1).isEmpty) None else Some(tokens(1)),
  //     drugId = if (tokens(2).isEmpty) None else Some(tokens(2)),
  //     mechanismOfAction = if (tokens(3).isEmpty) None else Some(StrSeqRep(tokens(3)).rep),
  //     isHighQuality = tokens(4).toBoolean,
  //     origin = if (tokens(5).isEmpty) None else Some(StrSeqRep(tokens(5)).rep),
  //     probeMinerScore = if (tokens(6).isEmpty) None else Some(tokens(6).toDouble),
  //     probesDrugsScore = if (tokens(7).isEmpty) None else Some(tokens(7).toDouble),
  //     scoreInCells = if (tokens(8).isEmpty) None else Some(tokens(8).toDouble),
  //     scoreInOrganisms = if (tokens(9).isEmpty) None else Some(tokens(9).toDouble),
  //     targetFromSourceId = tokens(10),
  //     urls = TupleSeqRep(tokens(11), chemicalProbeUrlParser).rep
  //   )
  // }
  // val chemicalProbeUrlParser: String => ChemicalProbeUrl = str => {
  //   val tokens = str.split(",")
  //   ChemicalProbeUrl(
  //     niceName = tokens(0),
  //     url = if (tokens(1).isEmpty) None else Some(tokens(1))
  //   )
  // }

  // implicit val cancerHallmarkImpW: OWrites[CancerHallmark] = Json.writes[CancerHallmark]
  // implicit val cancerHallmarkImpR: Reads[CancerHallmark] =
  //   ((__ \ "description").read[String] and
  //     (__ \ "impact").readNullable[String] and
  //     (__ \ "label").read[String] and
  //     (__ \ "pmid").read[Long])(CancerHallmark.apply)

  // implicit val chemicalProbeUrlImp: OFormat[ChemicalProbeUrl] = Json.format[ChemicalProbeUrl]
  // implicit val chemicalProbeImp: OFormat[ChemicalProbe] = Json.format[ChemicalProbe]

  // implicit val hallmarkAttributeImpW: OWrites[HallmarkAttribute] = Json.writes[HallmarkAttribute]
  // implicit val hallmarkAttributeImpR: Reads[HallmarkAttribute] =
  //   ((__ \ "attribute_name").read[String] and
  //     (__ \ "description").read[String] and
  //     (__ \ "pmid").readNullable[Long])(HallmarkAttribute.apply)

  // implicit val hallmarksImpW: OWrites[Hallmarks] = Json.writes[Hallmarks]
  // implicit val hallmarksImpR: Reads[Hallmarks] =
  //   ((__ \ "cancerHallmarks").readWithDefault[Seq[CancerHallmark]](Seq.empty) and
  //     (__ \ "attributes").readWithDefault[Seq[HallmarkAttribute]](Seq.empty))(Hallmarks.apply)

  // implicit val geneOntologyImpW: OWrites[GeneOntology] = Json.writes[models.entities.GeneOntology]
  // implicit val geneOntologyImpR: Reads[models.entities.GeneOntology] =
  //   ((__ \ "id").read[String] and
  //     (__ \ "aspect").read[String] and
  //     (__ \ "evidence").read[String] and
  //     (__ \ "geneProduct").read[String] and
  //     (__ \ "source").read[String])(GeneOntology.apply)

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
