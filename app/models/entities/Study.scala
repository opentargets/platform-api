package models.entities

import play.api.Logging
import models.entities.Study.{LdPopulationStructure, Sample, SumStatQC}
import models.gql.StudyTypeEnum
import play.api.libs.json.*
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json
import slick.jdbc.GetResult

case class StudyQueryArgs(
    id: Seq[String] = Seq.empty,
    diseaseIds: Seq[String] = Seq.empty,
    enableIndirect: Boolean = false
)

case class Study(
    studyId: String,
    condition: Option[String],
    projectId: Option[String],
    studyType: Option[StudyTypeEnum.Value],
    traitFromSource: Option[String],
    geneId: Option[String],
    biosampleFromSourceId: Option[String],
    nSamples: Option[Int],
    summarystatsLocation: Option[String],
    hasSumstats: Option[Boolean],
    cohorts: Option[Seq[String]],
    initialSampleSize: Option[String],
    traitFromSourceMappedIds: Option[Seq[String]],
    diseaseIds: Option[Seq[String]],
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
    discoverySamples: Option[Seq[Sample]],
    nCases: Option[Int],
    analysisFlags: Option[Seq[String]],
    sumstatQCValues: Option[Seq[SumStatQC]],
    metaTotal: Int
)

object Study extends Logging {
  import sangria.macros.derive._
  implicit val studiesFromDB: GetResult[Study] =
    GetResult(r => Json.parse(r.<<[String]).as[Study])

  case class Sample(ancestry: Option[String], sampleSize: Option[Int])

  case class LdPopulationStructure(ldPopulation: Option[String], relativeSampleSize: Option[Double])

  case class SumStatQC(QCCheckName: String, QCCheckValue: Double)

  implicit val sampleF: OFormat[Sample] = Json.format[Sample]
  implicit val ldPopulationStructureF: OFormat[LdPopulationStructure] =
    Json.format[LdPopulationStructure]
  implicit val sumStatQCF: OFormat[SumStatQC] = Json.format[SumStatQC]
  implicit val studyF: OFormat[Study] = Json.format[Study]

}
