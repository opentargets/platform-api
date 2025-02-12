package models.entities

import models.Backend
import models.gql.StudyTypeEnum
import models.gql.Arguments.StudyType
import models.entities.Loci.lociImp
import models.gql.Fetchers.{studyFetcher, targetsFetcher, variantFetcher}
import models.gql.ColocalisationsDeferred
import models.gql.LocusDeferred
import models.gql.L2GPredictionsDeferred
import models.gql.Objects.{colocalisationsImp, l2GPredictionsImp, logger, targetImp, variantIndexImp, studyImp}
import play.api.Logging
import play.api.libs.json.*
import play.api.libs.functional.syntax.*
import sangria.schema.{DeferredFutureValue, DeferredValue, Field, FloatType, IntType, ListType, ObjectType, OptionType, StringType, fields}
import models.gql.Arguments.{pageArg, pageSize, studyTypes, variantIds}
import sangria.macros.derive.{AddFields, ObjectTypeName, ReplaceField, deriveObjectType}

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

implicit val ldSetImp: ObjectType[Backend, LdSet] =
  deriveObjectType[Backend, LdSet]()


object CredibleSet extends Logging {
  import sangria.macros.derive._

  implicit val ldSetF: OFormat[LdSet] = Json.format[LdSet]

  import models.{EnumFormat, EnumMacros}

//  implicit val studyTypeEnum: EnumFormat[StudyTypeEnum.Value] = EnumFormat.derived[StudyTypeEnum.Value]

  implicit val credibleSetF: OFormat[CredibleSet] = Json.format[CredibleSet]
}
