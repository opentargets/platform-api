package models.entities

import play.api.Logging
import play.api.libs.json._

case class L2GPredictions(
    studyLocusId: String,
    geneId: String,
    score: Double
)

object L2GPredictions extends Logging {
  implicit val l2GPredictionsF: OFormat[L2GPredictions] = Json.format[L2GPredictions]
}
