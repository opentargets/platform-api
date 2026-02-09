package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import slick.jdbc.GetResult

case class HPO(
    id: String,
    name: String,
    description: Option[String],
    namespace: Option[Seq[String]]
)

object HPO {
  implicit val getHPOResult: GetResult[HPO] =
    GetResult(r => Json.parse(r.<<[String]).as[HPO])
  implicit val hpoImpF: OFormat[HPO] = Json.format[models.entities.HPO]
}
