package models.entities

import play.api.libs.json.{JsError, JsString, JsSuccess, JsValue, Json, OFormat, Reads, Writes}
import slick.jdbc.GetResult
import utils.OTLogging
import utils.db.DbJsonParser.fromPositionedResult

enum ClinicalReportType {
  case CURATED_RESOURCE, DRUG_LABEL, CLINICAL_TRIAL, REGULATORY_AGENCY
}

case class ClinRepDrugListItem(drugFromSource: Option[String], drugId: Option[String])

case class ClinicalReport(
    id: String,
    source: String,
    clinicalStage: String,
    phaseFromSource: Option[String],
    `type`: Option[ClinicalReportType],
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
    qualityControls: Seq[String],
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
    GetResult(fromPositionedResult[ClinicalReport])

  implicit val diseaseListItemW: Writes[ClinicalDiseaseListItem] =
    Json.writes[ClinicalDiseaseListItem]

  implicit val diseaseListItemF: OFormat[ClinicalDiseaseListItem] =
    Json.format[ClinicalDiseaseListItem]

  implicit val drugListItemF: OFormat[ClinRepDrugListItem] = Json.format[ClinRepDrugListItem]

  implicit val clinicalReportTypeWrites: Writes[ClinicalReportType] =
    Writes(t => JsString(t.toString))

  implicit val clinicalReportTypeReads: Reads[ClinicalReportType] = Reads {
    case JsString(s) =>
      ClinicalReportType.values.find(_.toString == s) match {
        case Some(v) => JsSuccess(v)
        case None    => JsError(s"Invalid ClinicalReportType: $s")
      }
    case _ => JsError("ClinicalReportType must be a string")
  }

  implicit val clinicalReportF: OFormat[ClinicalReport] = Json.format[ClinicalReport]
}
