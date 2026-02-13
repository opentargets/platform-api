package models.entities

import play.api.libs.json._
import slick.jdbc.GetResult
import utils.OTLogging

case class Reference(ids: Option[Seq[String]], source: String, urls: Option[Seq[String]])

case class MechanismsOfAction(
    chemblId: String,
    rows: Seq[MechanismOfActionRow],
    uniqueActionTypes: Seq[String],
    uniqueTargetTypes: Seq[String]
)

case class MechanismOfActionRow(
    mechanismOfAction: String,
    actionType: Option[String],
    targetName: Option[String],
    targets: Option[Seq[String]],
    references: Option[Seq[Reference]]
)

object MechanismsOfAction extends OTLogging {
  implicit val getFromDB: GetResult[MechanismsOfAction] =
    GetResult(r => Json.parse(r.<<[String]).as[MechanismsOfAction])
  implicit val referenceF: OFormat[Reference] = Json.format[Reference]
  implicit val mechanismOfActionRowF: OFormat[MechanismOfActionRow] =
    Json.format[MechanismOfActionRow]
  implicit val mechanismsOfActionF: OFormat[MechanismsOfAction] = Json.format[MechanismsOfAction]
}
