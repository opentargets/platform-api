package models.entities

import play.api.libs.json.*
import models.gql.TypeWithId
import org.slf4j.{Logger, LoggerFactory}

case class L2GPrediction(
    studyLocusId: String,
    geneId: String,
    score: Double,
    features: Option[Seq[L2GFeature]],
    shapBaseValue: Double
)

case class L2GFeature(name: String, value: Double, shapValue: Double)

object L2GPrediction {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

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
