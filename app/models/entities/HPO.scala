package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._

case class HPO(id: String, name: String, description: Option[String], namespace: Option[Seq[String]])

object HPO {
  implicit val hpoImpF = Json.format[models.entities.HPO]
}