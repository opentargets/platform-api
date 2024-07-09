package models.entities

import models.Backend
import models.gql.Fetchers.{diseasesFetcher, targetsFetcher}
import play.api.Logging
import play.api.libs.json.{JsValue, Json, OFormat}
import models.gql.Objects.{diseaseImp, targetImp}
import sangria.schema.{
  BooleanType,
  Field,
  IntType,
  ListType,
  ObjectType,
  OptionType,
  StringType,
  fields
}

object GwasIndex extends Logging {
  import sangria.macros.derive._

  case class Sample(ancestry: Option[String], sampleSize: Option[Int])

  case class LdPopulationStructure(ldPopulation: Option[String], relativeSampleSize: Option[Double])

  implicit val sampleF: OFormat[Sample] = Json.format[Sample]
  implicit val ldPopulationStructureF: OFormat[LdPopulationStructure] =
    Json.format[LdPopulationStructure]

  implicit val ldPopulationStructureImp: ObjectType[Backend, LdPopulationStructure] =
    deriveObjectType[Backend, LdPopulationStructure]()
  implicit val sampleImp: ObjectType[Backend, Sample] = deriveObjectType[Backend, Sample]()

  val gwasImp: ObjectType[Backend, JsValue] = ObjectType(
    "Gwas",
    "A genome-wide association study",
    fields[Backend, JsValue](
      Field(
        "studyId",
        StringType,
        description = Some("The study identifier"),
        resolve = js => (js.value \ "studyId").as[String]
      ),
      Field(
        "projectId",
        OptionType(StringType),
        description = Some("The project identifier"),
        resolve = js => (js.value \ "projectId").asOpt[String]
      ),
      Field(
        "studyType",
        OptionType(StringType),
        description = Some("The study type"),
        resolve = js => (js.value \ "studyType").asOpt[String]
      ),
      Field(
        "traitFromSource",
        OptionType(StringType),
        description = Some(""),
        resolve = js => (js.value \ "traitFromSource").asOpt[String]
      ),
      Field(
        "target",
        OptionType(targetImp),
        Some("Target"),
        resolve = js => {
          val geneId = (js.value \ "geneId").asOpt[String]
          logger.debug(s"Finding target: $geneId")
          targetsFetcher.deferOpt(geneId)
        }
      ),
      Field(
        "biosampleFromSourceId",
        OptionType(StringType),
        description = Some(""),
        resolve = js => (js.value \ "biosampleFromSourceId").asOpt[String]
      ),
      Field(
        "nSamples",
        OptionType(IntType),
        description = Some(""),
        resolve = js => (js.value \ "nSamples").asOpt[Int]
      ),
      Field(
        "summarystatsLocation",
        OptionType(StringType),
        description = Some(""),
        resolve = js => (js.value \ "summarystatsLocation").asOpt[String]
      ),
      Field(
        "hasSumstats",
        OptionType(BooleanType),
        description = Some(""),
        resolve = js => (js.value \ "hasSumstats").asOpt[Boolean]
      ),
      Field(
        "cohorts",
        OptionType(ListType(StringType)),
        description = Some(""),
        resolve = js => (js.value \ "cohorts").asOpt[Seq[String]]
      ),
      Field(
        "initialSampleSize",
        OptionType(StringType),
        description = Some(""),
        resolve = js => (js.value \ "initialSampleSize").asOpt[String]
      ),
      Field(
        "diseases",
        OptionType(ListType(diseaseImp)),
        None,
        resolve = js => {
          val ids = (js.value \ "traitFromSourceMappedIds").asOpt[Seq[String]].getOrElse(Seq.empty)
          logger.debug(s"Finding diseases for ids: $ids")
          diseasesFetcher.deferSeqOpt(ids)
        }
      ),
      Field(
        "publicationJournal",
        OptionType(StringType),
        description = Some(""),
        resolve = js => (js.value \ "publicationJournal").asOpt[String]
      ),
      Field(
        "publicationDate",
        OptionType(StringType),
        description = Some(""),
        resolve = js => (js.value \ "publicationDate").asOpt[String]
      ),
      Field(
        "ldPopulationStructure",
        OptionType(ListType(ldPopulationStructureImp)),
        description = Some(""),
        resolve = js => (js.value \ "ldPopulationStructure").asOpt[Seq[LdPopulationStructure]]
      ),
      Field(
        "backgroundTraits",
        OptionType(ListType(diseaseImp)),
        None,
        resolve = js => {
          val ids = (js.value \ "backgroundTraitFromSourceMappedIds")
            .asOpt[Seq[String]]
            .getOrElse(Seq.empty)
          logger.debug(s"Finding diseases for ids: $ids")
          diseasesFetcher.deferSeqOpt(ids)
        }
      ),
      Field(
        "qualityControls",
        OptionType(ListType(StringType)),
        description = Some(""),
        resolve = js => (js.value \ "qualityControls").asOpt[Seq[String]]
      ),
      Field(
        "replicationSamples",
        OptionType(ListType(sampleImp)),
        description = Some(""),
        resolve = js => (js.value \ "replicationSamples").asOpt[Seq[Sample]]
      ),
      Field(
        "nControls",
        OptionType(IntType),
        description = Some(""),
        resolve = js => (js.value \ "nControls").asOpt[Int]
      ),
      Field(
        "pubmedId",
        OptionType(StringType),
        description = Some(""),
        resolve = js => (js.value \ "pubmedId").asOpt[String]
      ),
      Field(
        "publicationFirstAuthor",
        OptionType(StringType),
        description = Some(""),
        resolve = js => (js.value \ "publicationFirstAuthor").asOpt[String]
      ),
      Field(
        "publicationTitle",
        OptionType(StringType),
        description = Some(""),
        resolve = js => (js.value \ "publicationTitle").asOpt[String]
      ),
      Field(
        "discoverySamples",
        OptionType(ListType(sampleImp)),
        description = Some(""),
        resolve = js => (js.value \ "discoverySamples").asOpt[Seq[Sample]]
      ),
      Field(
        "nCases",
        OptionType(IntType),
        description = Some(""),
        resolve = js => (js.value \ "nCases").asOpt[Int]
      ),
      Field(
        "analysisFlags",
        OptionType(ListType(StringType)),
        description = Some(""),
        resolve = js => (js.value \ "analysisFlags").asOpt[Seq[String]]
      )
    )
  )
}