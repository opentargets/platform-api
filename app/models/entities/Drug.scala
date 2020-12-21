package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class WithdrawnNotice(classes: Option[Seq[String]],
                           countries: Option[Seq[String]],
                           reasons: Option[Seq[String]],
                           year: Option[Int])

case class Reference(ids: Option[Seq[String]],
                     source: String,
                     urls: Option[Seq[String]])

case class MechanismOfActionRow(mechanismOfAction: String,
                                targetName: Option[String],
                                targets: Option[Seq[String]],
                                references: Option[Seq[Reference]])

case class IndicationRow(maxPhaseForIndication: Long,
                         disease: String,
                         references: Option[Seq[Reference]])

case class LinkedIds(count: Int, rows: Seq[String])

case class Indications(id: String, indications: Seq[IndicationRow], count: Long)

case class MechanismsOfAction(id: String,
                              rows: Seq[MechanismOfActionRow],
                              uniqueActionTypes: Seq[String],
                              uniqueTargetTypes: Seq[String])

case class Drug(id: String,
                name: String,
                synonyms: Seq[String],
                tradeNames: Seq[String],
                childChemblIds: Option[Seq[String]],
                yearOfFirstApproval: Option[Int],
                drugType: String,
                isApproved: Option[Boolean],
                maximumClinicalTrialPhase: Option[Int],
                hasBeenWithdrawn: Boolean,
                withdrawnNotice: Option[WithdrawnNotice],
                //mechanismsOfAction: Option[MechanismsOfAction],
                approvedIndications: Option[Seq[String]],
                //indications: Option[Indications],
                linkedDiseases: Option[LinkedIds],
                linkedTargets: Option[LinkedIds],
                blackBoxWarning: Boolean,
                description: Option[String])

object Drug {
  implicit val linkedIdsImpW = Json.format[models.entities.LinkedIds]
  implicit val withdrawnNoticeImpW = Json.format[models.entities.WithdrawnNotice]
  implicit val referenceImpW = Json.format[models.entities.Reference]
  implicit val mechanismOfActionRowImpW = Json.format[models.entities.MechanismOfActionRow]
  implicit val mechanismOfActionImpW = Json.format[models.entities.MechanismsOfAction]
  implicit val indicationRowImpW = Json.format[models.entities.IndicationRow]
  implicit val indicationsImpW = Json.format[models.entities.Indications]
  implicit val drugImpW = Json.format[models.entities.Drug]
}
