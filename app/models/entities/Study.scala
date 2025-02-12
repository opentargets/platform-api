package models.entities

import models.Backend
import models.gql.Fetchers.{biosamplesFetcher, credibleSetFetcher, diseasesFetcher, targetsFetcher}
import play.api.Logging
import play.api.libs.json.{JsValue, Json, OFormat}
import models.entities.CredibleSetQueryArgs
import models.entities.Study.{LdPopulationStructure, Sample, SumStatQC}
import models.gql.Objects.{biosampleImp, credibleSetImp, diseaseImp, targetImp}
import sangria.schema.{
  BooleanType,
  DeferredValue,
  EnumType,
  Field,
  IntType,
  ListType,
  ObjectType,
  OptionType,
  StringType,
  fields,
  given
}
import models.gql.{CredibleSetsByStudyDeferred, StudyTypeEnum}
import models.gql.Arguments.{StudyType, pageArg}
import play.api.libs.json.*
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json
import sangria.macros.derive.{
  AddFields,
  DocumentField,
  ObjectTypeDescription,
  ObjectTypeName,
  ReplaceField,
  deriveObjectType
}

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
    sumstatQCValues: Option[Seq[SumStatQC]]
)

object Study extends Logging {
  import sangria.macros.derive._

  case class Sample(ancestry: Option[String], sampleSize: Option[Int])

  case class LdPopulationStructure(ldPopulation: Option[String], relativeSampleSize: Option[Double])

  case class SumStatQC(QCCheckName: String, QCCheckValue: Double)

  implicit val sampleF: OFormat[Sample] = Json.format[Sample]
  implicit val ldPopulationStructureF: OFormat[LdPopulationStructure] =
    Json.format[LdPopulationStructure]
  implicit val sumStatQCW: OWrites[SumStatQC] = Json.writes[SumStatQC]
  implicit val sumStatQCR: Reads[SumStatQC] = (
    (JsPath \ "key").read[String] and
      (JsPath \ "value").read[Double]
  )(SumStatQC.apply)

  implicit val ldPopulationStructureImp: ObjectType[Backend, LdPopulationStructure] =
    deriveObjectType[Backend, LdPopulationStructure]()
  implicit val sampleImp: ObjectType[Backend, Sample] = deriveObjectType[Backend, Sample]()
  implicit val sumStatQCImp: ObjectType[Backend, SumStatQC] = deriveObjectType[Backend, SumStatQC]()
//  implicit val studyTypeF: EnumType[StudyTypeEnum] = deriveEnumType[StudyTypeEnum]()

  implicit val studyF: OFormat[Study] = Json.format[Study]

}
