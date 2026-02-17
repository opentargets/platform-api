package models.entities

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult
import utils.OTLogging

case class ClinicalTarget(
    id: String,
    drugId: Option[String],
    targetId: Option[String],
    diseases: Seq[ClinicalDiseaseListItem],
    maxClinicalStage: String,
    clinicalReportIds: Seq[String]
)

object ClinicalTarget extends OTLogging {

  implicit val getClinicalTargetsFromDB: GetResult[ClinicalTarget] =
    GetResult(r => Json.parse(r.<<[String]).as[ClinicalTarget])

  implicit val clinicalDiseaseListItemF: OFormat[ClinicalDiseaseListItem] =
    Json.format[ClinicalDiseaseListItem]

  implicit val clinicalTargetsF: OFormat[ClinicalTarget] = Json.format[ClinicalTarget]
}
