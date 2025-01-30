package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AdverseEvent(
    name: String,
    meddraCode: Option[String],
    count: Long,
    logLR: Double,
    criticalValue: Double
)

case class AdverseEvents(count: Long, criticalValue: Double, rows: Seq[AdverseEvent])

object AdverseEvent {
  implicit val AdverseEventImpReader: Reads[AdverseEvent] = (
    (JsPath \ "event").read[String] and
      (JsPath \ "meddraCode").readNullable[String] and
      (JsPath \ "count").read[Long] and
      (JsPath \ "llr").read[Double] and
      (JsPath \ "critval").read[Double]
  )(AdverseEvent.apply)

  implicit val AdverseEventsImpReader: Reads[AdverseEvents] = (
    (JsPath \ "count").read[Long] and
      (JsPath \ "critval").read[Double] and
      (JsPath \ "rows").read[Seq[AdverseEvent]]
  )(AdverseEvents.apply)
}
