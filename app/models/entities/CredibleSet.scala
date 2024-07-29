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

case class StrongestLocus2gene(geneId: String, score:Double)

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
                       studyType: Option[String],
                       traitFromSourceMappedIds: Option[Seq[String]],
                       qtlGeneId: Option[String]
)

object CredibleSet extends Logging {
  implicit val ldSetF: OFormat[LdSet] = Json.format[LdSet]
  implicit val locusF: OFormat[Locus] = Json.format[Locus]
  implicit val strongestLocus2geneF: OFormat[StrongestLocus2gene] = Json.format[StrongestLocus2gene]
  implicit val credibleSetF: OFormat[CredibleSet] = Json.format[CredibleSet]
}

