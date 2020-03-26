package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class DDRelation(id: String, A: String, B: String,
                      countA: Long, countB: Long,
                      countAOrB: Long, countAndB: Long,
                      score: Double)

case class DDRelations(count: Long, rows: Seq[DDRelation])

object DDRelation {
  val logger = Logger(this.getClass)

  object JSONImplicits {
    implicit val adverseEventImpF = Json.format[DDRelation]
  }
}
