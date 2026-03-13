package models.entities

import play.api.libs.json._
import slick.jdbc.GetResult
import utils.db.DbJsonParser.fromPositionedResult

case class HPO(
    id: String,
    name: String,
    description: Option[String],
    namespace: Option[Seq[String]]
)

object HPO {
  implicit val getHPOResult: GetResult[HPO] =
    GetResult(fromPositionedResult[HPO])
  implicit val hpoImpF: OFormat[HPO] = Json.format[models.entities.HPO]
}
