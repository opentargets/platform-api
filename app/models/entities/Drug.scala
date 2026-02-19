package models.entities

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

case class IndicationReference(ids: Option[Seq[String]], source: String)

case class IndicationRow(
    maxPhaseForIndication: Double,
    disease: String,
    references: Option[Seq[IndicationReference]]
)

case class LinkedIds(count: Int, rows: Seq[String])

case class Indications(
    id: String,
    indications: Seq[IndicationRow],
    indicationCount: Long,
    approvedIndications: Option[Seq[String]]
)

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
  implicit val indicationReferenceImpW: OFormat[IndicationReference] =
    Json.format[models.entities.IndicationReference]
  implicit val indicationRowImpW: OFormat[IndicationRow] =
    Json.format[models.entities.IndicationRow]
  implicit val indicationsImpW: OFormat[Indications] = Json.format[models.entities.Indications]
  implicit val DrugXRefImpW: OFormat[DrugReferences] = Json.format[DrugReferences]
  implicit val drugImplicitR: Reads[Drug] = Json.reads[Drug]
  implicit val drugImplicitW: OWrites[Drug] = Json.writes[Drug]
}
