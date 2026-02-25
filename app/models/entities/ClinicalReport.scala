package models.entities

import play.api.libs.json.{JsString, JsValue, Json, OFormat, Writes}
import slick.jdbc.GetResult
import utils.OTLogging

case class ClinRepDrugListItem(drugFromSource: String, drugId: String)

case class ClinicalReport(
    id: String,
    source: String,
    clinicalStage: String,
    phaseFromSource: Option[String],
    `type`: Option[String],
    title: Option[String],
    trialStudyType: Option[String],
    trialDescription: Option[String],
    trialNumberOfArms: Option[Int],
    trialStartDate: Option[String],
    trialLiterature: Seq[String],
    trialOverallStatus: Option[String],
    trialWhyStopped: Option[String],
    trialPrimaryPurpose: Option[String],
    trialPhase: Option[String],
    trialStopReasonCategories: Seq[String],
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
    GetResult { r =>
      val raw = r.<<[String]
      val escaped = raw
        .replaceAll("""\\([*^<>&\[\]_~])""", "$1")
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replaceAll("""\\(nrt)""", "\\\\$1")
      val json = Json.parse(escaped)
      json.as[ClinicalReport]
    }

  implicit val diseaseListItemW: Writes[ClinicalDiseaseListItem] =
    Json.writes[ClinicalDiseaseListItem]

  implicit val diseaseListItemF: OFormat[ClinicalDiseaseListItem] =
    Json.format[ClinicalDiseaseListItem]

  implicit val drugListItemW: Writes[ClinRepDrugListItem] = Json.writes[ClinRepDrugListItem]

  implicit val drugListItemF: OFormat[ClinRepDrugListItem] = Json.format[ClinRepDrugListItem]

  implicit val clinicalReportW: Writes[ClinicalReport] = Json.writes[ClinicalReport]

  implicit val clinicalReportF: OFormat[ClinicalReport] = Json.format[ClinicalReport]
}
