package models.entities

import play.api.Logging
import play.api.libs.json.{Json, OFormat, OWrites}

case class Sample(ancestry: Option[String], sampleSize: Option[Int])

case class LdPopulationStructure(ldPopulation: Option[String], relativeSampleSize: Option[Double])

case class GwasIndex(
    studyId: String,
    projectId: Option[String],
    studyType: Option[String],
    traitFromSource: Option[String],
    geneId: Option[String],
    biosampleFromSourceId: Option[String],
    nSamples: Option[Int],
    summarystatsLocation: Option[String],
    hasSumstats: Option[Boolean],
    cohorts: Option[Seq[String]],
    initialSampleSize: Option[String],
    traitFromSourceMappedIds: Option[Seq[String]],
    publicationJournal: Option[String],
    publicationDate: Option[String],
    ldPopulationStructure: Option[Seq[LdPopulationStructure]],
    backgroundTraitFromSourceMappedIds: Option[Seq[String]],
    qualityControls: Option[Seq[String]],
    replicationSamples: Option[Seq[Sample]],
    nControls: Option[Int],
    pubmedId: Option[String],
    publicationFirstAuthor: Option[String],
    publicationTitle: Option[String],
//    discoverySamples: Option[Seq[Sample]],
//    nCases: Option[Int],
//    analysisFlags: Option[Seq[String]]
)

object GwasIndex extends Logging {
  implicit val sampleF: OFormat[Sample] = Json.format[Sample]
  implicit val ldPopulationStructureF: OFormat[LdPopulationStructure] =
    Json.format[LdPopulationStructure]
  implicit val gwasIndexF: OFormat[GwasIndex] = Json.format[GwasIndex]
}
