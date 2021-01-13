package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class DDRelation(id: String,
                      A: String,
                      B: String,
                      countA: Long,
                      countB: Long,
                      countAOrB: Long,
                      countAAndB: Long,
                      score: Double)

case class DDRelations(maxCountAOrB: Long, count: Long, rows: Seq[DDRelation])

object DDRelation {
  implicit val adverseEventImpF = Json.format[DDRelation]
}
