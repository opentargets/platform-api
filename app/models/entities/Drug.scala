package models.entities

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

case class DrugWarningReference(id: String, source: String, url: String)

case class DrugWarning(
    toxicityClass: Option[String],
    chemblIds: Option[Seq[String]],
    country: Option[String],
    description: Option[String],
    id: Option[Long],
    references: Option[Seq[DrugWarningReference]],
    warningType: String,
    year: Option[Int],
    efoTerm: Option[String],
    efoId: Option[String],
    efoIdForWarningClass: Option[String]
)

case class LinkedIds(count: Int, rows: Seq[String])

case class DrugReferences(source: String, ids: Seq[String])

case class Drug(
    id: String,
    name: String,
    synonyms: Seq[String],
    tradeNames: Seq[String],
    childChemblIds: Option[Seq[String]], // Gone?
    yearOfFirstApproval: Option[Int],
    drugType: String,
    isApproved: Option[Boolean],
    crossReferences: Option[Seq[DrugReferences]],
    parentId: Option[String],
    maximumClinicalTrialPhase: Option[Double],
    hasBeenWithdrawn: Boolean,
    linkedDiseases: Option[LinkedIds],
    linkedTargets: Option[LinkedIds],
    blackBoxWarning: Boolean,
    description: Option[String]
)

object Drug {
  implicit val linkedIdsImpW: OFormat[LinkedIds] = Json.format[models.entities.LinkedIds]
  implicit val drugWarningsReferenceImpR: Reads[models.entities.DrugWarningReference] = (
    (JsPath \ "ref_id").read[String] and
      (JsPath \ "ref_type").read[String] and
      (JsPath \ "ref_url").read[String]
  )(DrugWarningReference.apply)
  implicit val drugWarningReferenceImpW: OWrites[DrugWarningReference] =
    Json.writes[DrugWarningReference]
  implicit val drugWarningImpW: OWrites[models.entities.DrugWarning] = Json.writes[DrugWarning]
  implicit val drugWarningImpR: Reads[models.entities.DrugWarning] = (
    (JsPath \ "toxicityClass").readNullable[String] and
      (JsPath \ "chemblIds").readNullable[Seq[String]] and
      (JsPath \ "country").readNullable[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "id").readNullable[Long] and
      (JsPath \ "references").readNullable[Seq[DrugWarningReference]] and
      (JsPath \ "warningType").read[String] and
      (JsPath \ "year").readNullable[Int] and
      (JsPath \ "efo_term").readNullable[String] and
      (JsPath \ "efo_id").readNullable[String] and
      (JsPath \ "efo_id_for_warning_class").readNullable[String]
  )(DrugWarning.apply)
  implicit val DrugXRefImpW: OFormat[DrugReferences] = Json.format[DrugReferences]
  implicit val drugImplicitR: Reads[Drug] = Json.reads[Drug]
  implicit val drugImplicitW: OWrites[Drug] = Json.writes[Drug]
}
