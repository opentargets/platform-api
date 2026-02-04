package models.entities

import play.api.Logging
import play.api.libs.json._
import models.gql.TypeWithId
import slick.jdbc.GetResult

case class L2GPrediction(
    studyLocusId: String,
    geneId: String,
    score: Double,
    features: Option[Seq[L2GFeature]],
    shapBaseValue: Double
)

case class L2GFeature(name: String, value: Double, shapValue: Double)

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
  implicit val l2GPredictionsF: OFormat[L2GPredictions] = Json.format[L2GPredictions]
  implicit val getFromDB: GetResult[L2GPredictions] =
    GetResult(r => Json.parse(r.<<[String]).as[L2GPredictions])
}
