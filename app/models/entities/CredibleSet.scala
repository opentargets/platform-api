package models.entities

import models.gql.StudyTypeEnum
import play.api.Logging
import play.api.libs.json.*

case class LdSet(
    tagVariantId: Option[String],
    r2Overall: Option[Double]
)

case class CredibleSet(studyLocusId: String,
                       variantId: String,
                       chromosome: String,
                       position: Int,
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
                       confidence: Option[String],
                       isTransQtl: Option[Boolean]
)

case class CredibleSetQueryArgs(
    ids: Seq[String] = Seq.empty,
    studyIds: Seq[String] = Seq.empty,
    variantIds: Seq[String] = Seq.empty,
    studyTypes: Seq[StudyTypeEnum.Value] = Seq.empty,
    regions: Seq[String] = Seq.empty
)

object CredibleSet extends Logging {

  implicit val ldSetF: OFormat[LdSet] = Json.format[LdSet]

  implicit val credibleSetF: OFormat[CredibleSet] = Json.format[CredibleSet]
}
