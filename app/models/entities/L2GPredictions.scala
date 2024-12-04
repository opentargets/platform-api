package models.entities

import play.api.Logging
import play.api.libs.json._
import models.gql.TypeWithId

case class L2GPrediction(
    studyLocusId: String,
    geneId: String,
    score: Double,
    locusToGeneFeatures: Option[Seq[L2GFeature]]
)

case class L2GFeature(key: String, value: Double)

object L2GPrediction extends Logging {
  implicit val l2GFeatureF: OFormat[L2GFeature] = Json.format[L2GFeature]
  implicit val l2GPredictionF: OFormat[L2GPrediction] = Json.format[L2GPrediction]
}

case class L2GPredictions(
    count: Long,
    rows: IndexedSeq[L2GPrediction],
    id: String = ""
) extends TypeWithId

object L2GPredictions {
  def empty: L2GPredictions = L2GPredictions(0, IndexedSeq.empty)
}
