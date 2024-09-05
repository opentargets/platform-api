package models.entities

import models.Backend
import models.entities.GwasIndex.{gwasImp, gwasWithoutCredSetsImp}
import models.gql.Fetchers.{gwasFetcher, targetsFetcher, variantFetcher}
import models.gql.Objects.{logger, targetImp, variantIndexImp}
import play.api.Logging
import play.api.libs.json.{JsValue, Json, OFormat, OWrites}
import sangria.schema.{
  Field,
  FloatType,
  IntType,
  ListType,
  ObjectType,
  OptionType,
  StringType,
  fields
}

case class Locus(
    variantId: Option[String],
    posteriorProbability: Option[Double],
    pValueMantissa: Option[Double],
    pValueExponent: Option[Int],
    logBF: Option[Double],
    beta: Option[Double],
    standardError: Option[Double],
    is95CredibleSet: Option[Boolean],
    is99CredibleSet: Option[Boolean],
    r2Overall: Option[Double]
)

case class LdSet(
    tagVariantId: Option[String],
    r2Overall: Option[Double]
)

case class StrongestLocus2gene(geneId: String, score: Double)

case class CredibleSet(studyLocusId: String,
                       variantId: Option[String],
                       chromosome: Option[String],
                       position: Option[Int],
                       region: Option[String],
                       studyId: Option[String],
                       beta: Option[Double],
                       zScore: Option[Double],
                       pValueMantissa: Option[Double],
                       pValueExponent: Option[Int],
                       effectAlleleFrequencyFromSource: Option[Double],
                       standardError: Option[Double],
                       subStudyDescription: Option[String],
                       qualityControls: Option[Seq[String]],
                       finemappingMethod: Option[String],
                       credibleSetIndex: Option[Int],
                       credibleSetlog10BF: Option[Double],
                       purityMeanR2: Option[Double],
                       purityMinR2: Option[Double],
                       locusStart: Option[Int],
                       locusEnd: Option[Int],
                       locus: Option[Seq[Locus]],
                       sampleSize: Option[Int],
                       strongestLocus2gene: Option[StrongestLocus2gene],
                       ldSet: Option[Seq[LdSet]],
                       studyType: Option[StudyTypeEnum.Value],
                       qtlGeneId: Option[String]
)

case class CredibleSetQueryArgs(
    ids: Seq[String] = Seq.empty,
    studyIds: Seq[String] = Seq.empty,
    variantIds: Seq[String] = Seq.empty,
    studyTypes: Seq[StudyTypeEnum.Value] = Seq.empty,
    regions: Seq[String] = Seq.empty
)

object StudyTypeEnum extends Enumeration {
  type StudyType = Value
  val gwas, tuqtl, eqtl, pqtl, sqtl = Value
}

object CredibleSet extends Logging {
  import sangria.macros.derive._

  implicit val StudyType = deriveEnumType[StudyTypeEnum.Value]()

  implicit val strongestLocus2geneImp: ObjectType[Backend, StrongestLocus2gene] =
    deriveObjectType[Backend, StrongestLocus2gene](
      ReplaceField(
        "geneId",
        Field(
          "target",
          OptionType(targetImp),
          Some("Target"),
          resolve = r => targetsFetcher.deferOpt(r.value.geneId)
        )
      )
    )
  implicit val ldSetImp: ObjectType[Backend, LdSet] =
    deriveObjectType[Backend, LdSet]()
  implicit val locusImp: ObjectType[Backend, Locus] = deriveObjectType[Backend, Locus](
    ReplaceField(
      "variantId",
      Field(
        "variant",
        OptionType(variantIndexImp),
        description = None,
        resolve = r => {
          val variantId = (r.value.variantId)
          logger.debug(s"Finding variant index: $variantId")
          variantFetcher.deferOpt(variantId)
        }
      )
    )
  )

  implicit val ldSetF: OFormat[LdSet] = Json.format[LdSet]
  implicit val locusF: OFormat[Locus] = Json.format[Locus]
  implicit val strongestLocus2geneF: OFormat[StrongestLocus2gene] = Json.format[StrongestLocus2gene]
  val credibleSetFields: Seq[Field[Backend, JsValue]] = Seq(
    Field(
      "studyLocusId",
      StringType,
      description = None,
      resolve = js => (js.value \ "studyLocusId").as[String]
    ),
    Field(
      "variant",
      OptionType(variantIndexImp),
      description = None,
      resolve = js => {
        val id = (js.value \ "variantId").asOpt[String]
        logger.debug(s"Finding variant for id: $id")
        variantFetcher.deferOpt(id)
      }
    ),
    Field(
      "chromosome",
      OptionType(StringType),
      description = None,
      resolve = js => (js.value \ "chromosome").asOpt[String]
    ),
    Field(
      "position",
      OptionType(IntType),
      description = None,
      resolve = js => (js.value \ "position").asOpt[Int]
    ),
    Field(
      "region",
      OptionType(StringType),
      description = None,
      resolve = js => (js.value \ "region").asOpt[String]
    ),
    Field(
      "beta",
      OptionType(FloatType),
      description = None,
      resolve = js => (js.value \ "beta").asOpt[Double]
    ),
    Field(
      "zScore",
      OptionType(FloatType),
      description = None,
      resolve = js => (js.value \ "zScore").asOpt[Double]
    ),
    Field(
      "pValueMantissa",
      OptionType(FloatType),
      description = None,
      resolve = js => (js.value \ "pValueMantissa").asOpt[Double]
    ),
    Field(
      "pValueExponent",
      OptionType(IntType),
      description = None,
      resolve = js => (js.value \ "pValueExponent").asOpt[Int]
    ),
    Field(
      "effectAlleleFrequencyFromSource",
      OptionType(FloatType),
      description = None,
      resolve = js => (js.value \ "effectAlleleFrequencyFromSource").asOpt[Double]
    ),
    Field(
      "standardError",
      OptionType(FloatType),
      description = None,
      resolve = js => (js.value \ "standardError").asOpt[Double]
    ),
    Field(
      "subStudyDescription",
      OptionType(StringType),
      description = None,
      resolve = js => (js.value \ "subStudyDescription").asOpt[String]
    ),
    Field(
      "qualityControls",
      OptionType(ListType(StringType)),
      description = None,
      resolve = js => (js.value \ "qualityControls").asOpt[Seq[String]]
    ),
    Field(
      "finemappingMethod",
      OptionType(StringType),
      description = None,
      resolve = js => (js.value \ "finemappingMethod").asOpt[String]
    ),
    Field(
      "credibleSetIndex",
      OptionType(IntType),
      description = None,
      resolve = js => (js.value \ "credibleSetIndex").asOpt[Int]
    ),
    Field(
      "credibleSetlog10BF",
      OptionType(FloatType),
      description = None,
      resolve = js => (js.value \ "credibleSetlog10BF").asOpt[Double]
    ),
    Field(
      "purityMeanR2",
      OptionType(FloatType),
      description = None,
      resolve = js => (js.value \ "purityMeanR2").asOpt[Double]
    ),
    Field(
      "purityMinR2",
      OptionType(FloatType),
      description = None,
      resolve = js => (js.value \ "purityMinR2").asOpt[Double]
    ),
    Field(
      "locusStart",
      OptionType(IntType),
      description = None,
      resolve = js => (js.value \ "locusStart").asOpt[Int]
    ),
    Field(
      "locusEnd",
      OptionType(IntType),
      description = None,
      resolve = js => (js.value \ "locusEnd").asOpt[Int]
    ),
    Field(
      "locus",
      OptionType(ListType(locusImp)),
      description = None,
      resolve = js => (js.value \ "locus").asOpt[Seq[Locus]]
    ),
    Field(
      "sampleSize",
      OptionType(IntType),
      description = None,
      resolve = js => (js.value \ "sampleSize").asOpt[Int]
    ),
    Field(
      "strongestLocus2gene",
      OptionType(strongestLocus2geneImp),
      description = None,
      resolve = js => (js.value \ "strongestLocus2gene").asOpt[StrongestLocus2gene]
    ),
    Field(
      "ldSet",
      OptionType(ListType(ldSetImp)),
      description = None,
      resolve = js => (js.value \ "ldSet").asOpt[Seq[LdSet]]
    ),
    Field(
      "studyType",
      OptionType(StudyType),
      description = None,
      resolve = js => (js.value \ "studyType").asOpt[String].map(e => StudyTypeEnum.withName(e))
    ),
    Field(
      "qtlGeneId",
      OptionType(StringType),
      description = None,
      resolve = js => (js.value \ "qtlGeneId").asOpt[String]
    )
  )
  val studyField: Field[Backend, JsValue] = Field(
    "study",
    OptionType(gwasWithoutCredSetsImp),
    description = Some("Gwas study"),
    resolve = js => {
      val studyId = (js.value \ "studyId").asOpt[String]
      logger.debug(s"Finding gwas study: $studyId")
      gwasFetcher.deferOpt(studyId)
    }
  )
  val credibleSetImp: ObjectType[Backend, JsValue] = ObjectType(
    "credibleSet",
    "",
    fields[Backend, JsValue](
      credibleSetFields ++ Seq(studyField): _*
    )
  )
  val credibleSetWithoutStudyImp: ObjectType[Backend, JsValue] = ObjectType(
    "credibleSetWithoutStudy",
    "",
    fields[Backend, JsValue](
      credibleSetFields: _*
    )
  )
}
