package models.entities

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult
import utils.OTLogging

case class ClinRepDrugListItem(drugFromSource: String, drugId: String)

case class ClinicalReport(
    id: String,
    source: String,
    clinicalStage: String,
    phaseFromSource: Option[String],
    `type`: Option[String],
    trialStudyType: Option[String],
    trialDescription: Option[String],
    trialNumberOfArms: Option[Int],
    trialStartDate: Option[String],
    trialLiterature: Seq[String],
    trialOverallStatus: Option[String],
    trialWhyStopped: Option[String],
    trialPrimaryPurpose: Option[String],
    trialPhase: Option[String],
    diseases: Seq[ClinicalDiseaseListItem],
    drugs: Seq[ClinRepDrugListItem],
    hasExpertReview: Boolean,
    countries: Seq[String],
    year: Option[Int],
    sideEffects: Seq[ClinicalDiseaseListItem],
    trialOfficialTitle: Option[String],
    url: Option[String]
)

object ClinicalReport extends OTLogging {
  implicit val getClinicalReportFromDB: GetResult[ClinicalReport] =
    GetResult(r => Json.parse(r.<<[String]).as[ClinicalReport])

  implicit val diseaseListItemF: OFormat[ClinicalDiseaseListItem] =
    Json.format[ClinicalDiseaseListItem]

  implicit val drugListItemF: OFormat[ClinRepDrugListItem] = Json.format[ClinRepDrugListItem]

  implicit val clinicalReportF: OFormat[ClinicalReport] = Json.format[ClinicalReport]
}
