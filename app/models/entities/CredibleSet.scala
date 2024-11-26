package models.entities

import models.Backend
import models.gql.StudyTypeEnum
import models.gql.Arguments.StudyType
import models.entities.GwasIndex.{gwasImp, gwasWithoutCredSetsImp}
import models.entities.Loci.lociImp
import models.gql.Fetchers.{
  gwasFetcher,
  l2gFetcher,
  l2gByStudyLocusIdRel,
  targetsFetcher,
  variantFetcher
}
import models.gql.LocusDeferred
import models.gql.Objects.{logger, targetImp, variantIndexImp, colocalisationImp, l2gPredictionsImp}
import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.{Reads, JsValue, Json, OFormat, OWrites}
import play.api.libs.functional.syntax._
import sangria.schema.{
  Field,
  FloatType,
  IntType,
  ListType,
  ObjectType,
  OptionType,
  StringType,
  fields,
  DeferredValue,
  DeferredFutureValue
}
import models.gql.Arguments.{studyTypes, pageArg, pageSize, variantIds}

case class LdSet(
    tagVariantId: Option[String],
    r2Overall: Option[Double]
)

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
                       loci: Loci,
                       sampleSize: Option[Int],
                       ldSet: Option[Seq[LdSet]],
                       studyType: Option[StudyTypeEnum.Value],
                       qtlGeneId: Option[String],
                       confidence: Option[String]
)

case class CredibleSetQueryArgs(
    ids: Seq[String] = Seq.empty,
    studyIds: Seq[String] = Seq.empty,
    variantIds: Seq[String] = Seq.empty,
    studyTypes: Seq[StudyTypeEnum.Value] = Seq.empty,
    regions: Seq[String] = Seq.empty
)

object CredibleSet extends Logging {
  import sangria.macros.derive._

  implicit val ldSetImp: ObjectType[Backend, LdSet] =
    deriveObjectType[Backend, LdSet]()

  implicit val ldSetF: OFormat[LdSet] = Json.format[LdSet]

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
        val id = (js.value \ "variantId").as[String]
        logger.debug(s"Finding variant for id: $id")
        variantFetcher.deferOpt(id)
      }
    ),
    Field(
      "l2Gpredictions",
      OptionType(ListType(l2gPredictionsImp)),
      description = None,
      arguments = pageSize :: Nil,
      resolve = js => {
        import scala.concurrent.ExecutionContext.Implicits.global
        val id: String = (js.value \ "studyLocusId").as[String]
        val l2gValues = DeferredValue(l2gFetcher.deferRelSeq(l2gByStudyLocusIdRel, id))
        val t = js.arg(pageSize) match {
          case Some(size) =>
            l2gValues.map(_.take(size))
          case None =>
            l2gValues
        }
        t
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
      lociImp,
      arguments = variantIds :: pageArg :: Nil,
      description = None,
      resolve = js => {
        import scala.concurrent.ExecutionContext.Implicits.global
        val id = (js.value \ "studyLocusId").as[String]
        LocusDeferred(id, js.arg(variantIds), js.arg(pageArg))
      }
    ),
    Field(
      "sampleSize",
      OptionType(IntType),
      description = None,
      resolve = js => (js.value \ "sampleSize").asOpt[Int]
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
    ),
    Field(
      "colocalisation",
      OptionType(ListType(colocalisationImp)),
      description = None,
      arguments = studyTypes :: pageArg :: Nil,
      resolve = js => {
        val id = (js.value \ "studyLocusId").as[String]
        js.ctx.getColocalisation(id, js.arg(studyTypes), js.arg(pageArg))
      }
    ),
    Field(
      "confidence",
      OptionType(StringType),
      description = None,
      resolve = js => (js.value \ "confidence").asOpt[String]
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
