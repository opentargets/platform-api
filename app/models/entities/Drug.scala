package models.entities

import play.api.Logger
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
                                targetName: String,
                                targets: Seq[String],
                                references: Option[Seq[Reference]])

case class IndicationRow(maxPhaseForIndication: Long,
                      disease: String,
                      references: Option[Seq[Reference]])

case class LinkedDiseases(count: Int, rows: Seq[String])
case class LinkedTargets(count: Int, rows: Seq[String])

case class Indications(rows: Seq[IndicationRow], count: Long)

case class MechanismsOfAction(rows: Seq[MechanismOfActionRow],
                             uniqueActionTypes: Seq[String],
                             uniqueTargetTypes: Seq[String])

case class Drug(id: String,
                name: String,
                synonyms: Seq[String],
                tradeNames: Seq[String],
                yearOfFirstApproval: Option[Int],
                drugType: String,
                maximumClinicalTrialPhase: Option[Int],
                hasBeenWithdrawn: Boolean,
                withdrawnNotice: Option[WithdrawnNotice],
                internalCompound: Boolean,
                mechanismsOfAction: Option[MechanismsOfAction],
                indications: Option[Indications],
                linkedDiseases: LinkedDiseases,
                linkedTargets: LinkedTargets,
                blackBoxWarning: Boolean,
                description: String)

object Drug {
  val logger = Logger(this.getClass)
  object JSONImplicits {
    implicit val linkedDiseasesImpW = Json.format[models.entities.LinkedDiseases]
    implicit val linkedTargetsImpW = Json.format[models.entities.LinkedTargets]
    implicit val withdrawnNoticeImpW = Json.format[models.entities.WithdrawnNotice]
    implicit val referenceImpW = Json.format[models.entities.Reference]
    implicit val mechanismOfActionRowImpW = Json.format[models.entities.MechanismOfActionRow]
    implicit val mechanismOfActionImpW = Json.format[models.entities.MechanismsOfAction]
    implicit val indicationRowImpW = Json.format[models.entities.IndicationRow]
    implicit val indicationsImpW = Json.format[models.entities.Indications]
    implicit val drugImpW = Json.format[models.entities.Drug]
  }

  def fromJsValue(jObj: JsValue): Option[Drug] = {
    /* apply transformers for json and fill the target
     start from internal objects and then map the external
     */
    import Drug.JSONImplicits._
    val source = (__ \ '_source).json.pick
    jObj.transform(source).asOpt.map(obj => {
      logger.debug(Json.prettyPrint(obj))
      obj.as[models.entities.Drug]
    })
  }
}
