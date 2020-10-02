package models.entities

import play.api.libs.json._

case class InteractionEvidence(id: String)

case class Interaction(intA: String, targetA: String,
                       intB: String, targetB: String,
                       intABiologicalRole: String,
                       intBBiologicalRole: String,
                       scoring: Option[Double],
                       count: Long,
                       sourceDatabase: String)

case class Interactions(count: Long, rows: IndexedSeq[Interaction])


object Interactions {
  implicit val interactionEvidenceJSONImp = Json.format[InteractionEvidence]
  implicit val interactionJSONImp = Json.format[Interaction]
  implicit val interactionsJSONImp = Json.format[Interactions]
}


