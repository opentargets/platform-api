package models.entities

import play.api.libs.json.{Json, OFormat}
import utils.OTLogging
import slick.jdbc.GetResult
import utils.db.DbJsonParser.fromPositionedResult

case class KeyValuePair(key: String, value: String)
case class TargetPrioritisation(targetId: String, items: Seq[KeyValuePair])

object TargetPrioritisation extends OTLogging {
  implicit val getTargetPrioritisationResult: GetResult[TargetPrioritisation] =
    GetResult(fromPositionedResult[TargetPrioritisation])
  implicit val targetPrioritisationF: OFormat[TargetPrioritisation] =
    Json.format[TargetPrioritisation]
  implicit val keyValuePairF: OFormat[KeyValuePair] = Json.format[KeyValuePair]
}
