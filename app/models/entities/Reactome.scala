package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.JsonNaming.SnakeCase

case class Reactome(id: String,
                    label: String,
                    children: Seq[String],
                    parents: Seq[String],
                    ancestors: Seq[String]) {
  val isRoot: Boolean = parents.length <= 1 && parents.headOption.forall(_ == "root")
}

object Reactome {
  implicit val reactomeNodeImpW: OWrites[Reactome] = Json.writes[Reactome]

  implicit val config: JsonConfiguration.Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
  implicit val reactomeNodeImpR: Reads[Reactome] = Json.reads[Reactome]
}
