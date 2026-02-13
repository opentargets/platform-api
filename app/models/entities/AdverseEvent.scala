package models.entities

import play.api.libs.json._
import slick.jdbc.GetResult
import models.gql.TypeWithId

case class AdverseEvent(
    name: String,
    meddraCode: Option[String],
    count: Long,
    logLR: Double
)

case class AdverseEvents(count: Long,
                         criticalValue: Double,
                         rows: Seq[AdverseEvent],
                         id: String = ""
) extends TypeWithId

object AdverseEvent {
  implicit val getRowFromDB: GetResult[AdverseEvents] =
    GetResult(r => Json.parse(r.<<[String]).as[AdverseEvents])
  implicit val adverseEventF: OFormat[AdverseEvent] = Json.format[AdverseEvent]
  implicit val adverseEventsF: OFormat[AdverseEvents] = Json.format[AdverseEvents]
}
