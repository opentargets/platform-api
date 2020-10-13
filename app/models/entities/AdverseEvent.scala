package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AdverseEvent(name: String, count: Long, logLR: Double, criticalValue: Double)

case class AdverseEvents(count: Long, criticalValue: Double, rows: Seq[AdverseEvent])

object AdverseEvent {
  implicit val adverseEventImpJSON = Json.format[AdverseEvent]
}
