package models.entities

import play.api.Logging
import play.api.libs.json.{OFormat, OWrites, Json}

case class InSilicoPredictor(method: String,
                             assessment: String,
                             score: Double,
                             assessmentFlag: String,
                             targetId: String)


case class TranscriptConsequence(variantConsequenceIds: Seq[String],
                                 amino_acid_change: String,
                                 uniprotAccessions: Seq[String],
                                 isEnsemblCanonical: Boolean,
                                 codons: String,
                                 distance: Double,
                                 targetId: String,
                                 impact: String,
                                 transcriptId: String,
                                 lofteePrediction: String,
                                 siftPrediction: Double,
                                 polyphenPrediction: Double)

case class DbXref(id: String, source: String)

case class AlleleFrequency(populationName: String, alleleFrequency: Double)

case class VariantIndex(variantId: String,
                       chromosome: String,
                       position: Int,
                       referenceAllele: String,
                       alternateAllele: String,
                       inSilicoPredictors: Seq[InSilicoPredictor],
                       mostSevereConsequenceId: String,
                       transcriptConsequences: Seq[TranscriptConsequence],
                       rsIds: Seq[String],
                       dbXrefs: Seq[DbXref],
                       alleleFrequencies: Seq[AlleleFrequency])

object VariantIndex extends Logging {
  implicit val inSilicoPredictorF: OFormat[InSilicoPredictor] = Json.format[InSilicoPredictor]
  implicit val transcriptConsequenceF: OFormat[TranscriptConsequence] = Json.format[TranscriptConsequence]
  implicit val dbXrefF: OFormat[DbXref] = Json.format[DbXref]
  implicit val alleleFrequencyF: OFormat[AlleleFrequency] = Json.format[AlleleFrequency]
  implicit val variantIndexF: OFormat[VariantIndex] = Json.format[VariantIndex]
}
