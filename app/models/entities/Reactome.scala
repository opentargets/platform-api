package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonNaming.SnakeCase

case class Reactome(id: String, label: String,
                    children: Seq[String],
                    parents: Seq[String],
                    ancestors: Seq[String]) {
  val isRoot: Boolean = parents.length <= 1 && parents.headOption.forall(_ == "root")
}

object Reactome {
  implicit val reactomeNodeImpW = Json.writes[Reactome]

  implicit val config = JsonConfiguration(SnakeCase)
  implicit val reactomeNodeImpR = Json.reads[Reactome]
}
