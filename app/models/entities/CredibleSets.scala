package models.entities

import models.gql.TypeWithId
import models.gql.StudyTypeEnum
import play.api.Logging
import play.api.libs.json._
import slick.jdbc.GetResult

case class CredibleSets(
    count: Long,
    rows: IndexedSeq[CredibleSet],
    id: String = ""
) extends TypeWithId

case class LdSet(
    tagVariantId: String,
    r2Overall: Double
)

case class CredibleSet(studyLocusId: String,
                       variantId: String,
                       chromosome: String,
                       position: Int,
                       region: Option[String],
                       studyId: String,
                       beta: Option[Double],
                       zScore: Option[Double],
                       pValueMantissa: Double,
                       pValueExponent: Int,
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
                       studyType: StudyTypeEnum.Value,
                       qtlGeneId: Option[String],
                       confidence: Option[String],
                       isTransQtl: Option[Boolean],
                       metaTotal: Int = 0,
                       metaGroupId: Option[String] = None
)

case class CredibleSetQueryArgs(
    ids: Seq[String] = Seq.empty,
    studyIds: Seq[String] = Seq.empty,
    variantIds: Seq[String] = Seq.empty,
    studyTypes: Seq[StudyTypeEnum.Value] = Seq.empty,
    regions: Seq[String] = Seq.empty
)

object CredibleSets extends Logging {
  def empty: CredibleSets = CredibleSets(0, IndexedSeq.empty)
  implicit val getResultCredibleSet: GetResult[CredibleSet] =
    GetResult(r => Json.parse(r.<<[String]).as[CredibleSet])
  implicit val ldSetF: OFormat[LdSet] = Json.format[LdSet]

  implicit val credibleSetF: OFormat[CredibleSet] = Json.format[CredibleSet]
}
