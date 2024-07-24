package models.entities

import play.api.Logging
import play.api.libs.json.{OFormat, OWrites, Json}

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
case class CredibleSet(studyLocusId: Long,
                       variantId: Option[String],
                       chromosome: Option[String],
                       position: Option[Int],
                       region: Option[String],
                       studyId: Option[String],
                       beta: Option[Double],
                       pValueMantissa: Option[Double],
                       pValueExponent: Option[Int],
                       standardError: Option[Double],
                       finemappingMethod: Option[String],
                       credibleSetIndex: Option[Int],
                       locus: Option[Seq[Locus]],
                       credibleSetlog10BF: Option[Double],
                       effectAlleleFrequencyFromSource: Option[Double],
                       zScore: Option[Double],
                       subStudyDescription: Option[String],
                       qualityControls: Option[Seq[String]],
                       purityMeanR2: Option[Double],
                       purityMinR2: Option[Double],
                       sampleSize: Option[Int],
                       ldSet: Option[Seq[LdSet]]
)

object CredibleSet extends Logging {
  implicit val ldSetF: OFormat[LdSet] = Json.format[LdSet]
  implicit val locusF: OFormat[Locus] = Json.format[Locus]
  implicit val credibleSetF: OFormat[CredibleSet] = Json.format[CredibleSet]
}
