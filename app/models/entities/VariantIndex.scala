package models.entities

import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{Json, OFormat, OWrites}

case class VariantEffect(method: Option[String],
                         assessment: Option[String],
                         score: Option[Double],
                         assessmentFlag: Option[String],
                         targetId: Option[String],
                         normalisedScore: Option[Double]
)

case class TranscriptConsequence(variantFunctionalConsequenceIds: Option[Seq[String]],
                                 aminoAcidChange: Option[String],
                                 uniprotAccessions: Option[Seq[String]],
                                 isEnsemblCanonical: Boolean,
                                 codons: Option[String],
                                 distanceFromFootprint: Int,
                                 distanceFromTss: Int,
                                 targetId: Option[String],
                                 impact: Option[String],
                                 transcriptId: Option[String],
                                 lofteePrediction: Option[String],
                                 siftPrediction: Option[Double],
                                 polyphenPrediction: Option[Double],
                                 transcriptIndex: Long,
                                 consequenceScore: Double
)

case class DbXref(id: Option[String], source: Option[String])

case class AlleleFrequency(populationName: Option[String], alleleFrequency: Option[Double])

case class VariantIndex(variantId: String,
                        chromosome: String,
                        position: Int,
                        referenceAllele: String,
                        alternateAllele: String,
                        variantEffect: Option[Seq[VariantEffect]],
                        mostSevereConsequenceId: String,
                        transcriptConsequences: Option[Seq[TranscriptConsequence]],
                        rsIds: Option[Seq[String]],
                        dbXrefs: Option[Seq[DbXref]],
                        alleleFrequencies: Option[Seq[AlleleFrequency]],
                        hgvsId: Option[String],
                        variantDescription: String
)

object VariantIndex {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val variantEffectF: OFormat[VariantEffect] = Json.format[VariantEffect]
  implicit val transcriptConsequenceF: OFormat[TranscriptConsequence] =
    Json.format[TranscriptConsequence]
  implicit val dbXrefF: OFormat[DbXref] = Json.format[DbXref]
  implicit val alleleFrequencyF: OFormat[AlleleFrequency] = Json.format[AlleleFrequency]
  implicit val variantIndexF: OFormat[VariantIndex] = Json.format[VariantIndex]
}
