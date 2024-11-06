package models.entities

import play.api.Logging
import play.api.libs.json._

case class L2GPredictions(
    studyLocusId: String,
    geneId: String,
    score: Double,
    locusToGeneFeatures: Option[Seq[L2GFeature]]
)

case class L2GFeature(key: String, value: Double)

object L2GPredictions extends Logging {
  implicit val l2GFeatureF: OFormat[L2GFeature] = Json.format[L2GFeature]
  implicit val l2GPredictionsF: OFormat[L2GPredictions] = Json.format[L2GPredictions]
}
