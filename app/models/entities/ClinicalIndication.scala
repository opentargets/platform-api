package models.entities

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult
import utils.OTLogging

case class ClinicalIndication(
    id: String,
    drugId: Option[String],
    diseaseId: Option[String],
    maxClinicalStage: String,
    clinicalReportIds: Seq[String]
)

object ClinicalIndication extends OTLogging {

  implicit val getClinicalIndicationsFromDB: GetResult[ClinicalIndication] =
    GetResult(r => Json.parse(r.<<[String]).as[ClinicalIndication])

  implicit val clinicalIndicationsF: OFormat[ClinicalIndication] = Json.format[ClinicalIndication]
}
