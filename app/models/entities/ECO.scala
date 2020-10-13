package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class ECO(id: String, label: String)

object ECO {
  implicit val ecoImpF = Json.format[models.entities.ECO]
}
