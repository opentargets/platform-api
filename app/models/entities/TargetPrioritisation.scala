package models.entities

import play.api.libs.json.{Json, OFormat}
import utils.OTLogging
import slick.jdbc.GetResult

case class KeyValuePair(key: String, value: String)
case class TargetPrioritisation(targetId: String, items: Seq[KeyValuePair])

object TargetPrioritisation extends OTLogging {
  implicit val getTargetPrioritisationResult: GetResult[TargetPrioritisation] =
    GetResult(r => Json.parse(r.<<[String]).as[TargetPrioritisation])
  implicit val targetPrioritisationF: OFormat[TargetPrioritisation] =
    Json.format[TargetPrioritisation]
  implicit val keyValuePairF: OFormat[KeyValuePair] = Json.format[KeyValuePair]
}
