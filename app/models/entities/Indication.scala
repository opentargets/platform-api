package models.entities

import play.api.libs.json._
import slick.jdbc.GetResult

case class IndicationReference(ids: Option[Seq[String]], source: String)

case class IndicationRow(
    maxPhaseForIndication: Double,
    disease: String,
    references: Option[Seq[IndicationReference]]
)

case class Indications(
    id: String,
    indications: Seq[IndicationRow],
    indicationCount: Long,
    approvedIndications: Option[Seq[String]]
)

object Indications {
  implicit val getFromDb: GetResult[Indications] =
    GetResult(r => Json.parse(r.<<[String]).as[Indications])
  implicit val indicationReferenceF: OFormat[IndicationReference] = Json.format[IndicationReference]
  implicit val indicationRowF: OFormat[IndicationRow] = Json.format[IndicationRow]
  implicit val indicationsF: OFormat[Indications] = Json.format[Indications]
}
