package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AdverseEvent(name: String, count: Long, llr: Double, criticalValue: Double)
case class AdverseEvents(count: Long, critVal: Double, rows: Seq[AdverseEvent])

object AdverseEvent {
  val logger = Logger(this.getClass)

  object JSONImplicits {
    implicit val adverseEventImpW = Json.writes[AdverseEvent]
    implicit val adverseEventImpR: Reads[AdverseEvent] =
      ((JsPath \ "event").read[String] and
        (JsPath \ "count").read[Long] and
        (JsPath \ "llr").read[Double] and
        (JsPath \ "critval").read[Double]
      )(AdverseEvent.apply _)
  }
}
