package models.entities

import play.api.Logging
import play.api.libs.json.{OFormat, OWrites, Json}

case class InSilicoPredictor(method: Option[String],
                             assessment: Option[String],
                             score: Option[Double],
                             assessmentFlag: Option[String],
                             targetId: Option[String]
)

case class TranscriptConsequence(variantFunctionalConsequenceIds: Option[Seq[String]],
                                 amino_acid_change: Option[String],
                                 uniprotAccessions: Option[Seq[String]],
                                 isEnsemblCanonical: Boolean,
                                 codons: Option[String],
                                 distance: Option[Double],
                                 targetId: Option[String],
                                 impact: Option[String],
                                 transcriptId: Option[String],
                                 lofteePrediction: Option[String],
                                 siftPrediction: Option[Double],
                                 polyphenPrediction: Option[Double]
)

case class DbXref(id: Option[String], source: Option[String])

case class AlleleFrequency(populationName: Option[String], alleleFrequency: Option[Double])

case class VariantIndex(variantId: String,
                        chromosome: String,
                        position: Int,
                        referenceAllele: String,
                        alternateAllele: String,
                        inSilicoPredictors: Option[Seq[InSilicoPredictor]],
                        mostSevereConsequenceId: String,
                        transcriptConsequences: Option[Seq[TranscriptConsequence]],
                        rsIds: Option[Seq[String]],
                        dbXrefs: Option[Seq[DbXref]],
                        alleleFrequencies: Option[Seq[AlleleFrequency]]
)

object VariantIndex extends Logging {
  implicit val inSilicoPredictorF: OFormat[InSilicoPredictor] = Json.format[InSilicoPredictor]
  implicit val transcriptConsequenceF: OFormat[TranscriptConsequence] =
    Json.format[TranscriptConsequence]
  implicit val dbXrefF: OFormat[DbXref] = Json.format[DbXref]
  implicit val alleleFrequencyF: OFormat[AlleleFrequency] = Json.format[AlleleFrequency]
  implicit val variantIndexF: OFormat[VariantIndex] = Json.format[VariantIndex]
}
