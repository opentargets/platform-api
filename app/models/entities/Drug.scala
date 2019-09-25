package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

//type DrugDetails {
//  mechanismsOfAction: DrugDetailMechanismsOfAction
//  linkedTargets: DrugDetailLinkedTargets
//  linkedDiseases: DrugDetailLinkedDiseases
//}

case class WithdrawnNotice(classes: Option[Seq[String]],
                            countries: Option[Seq[String]],
                            reasons: Option[Seq[String]],
                            year: Option[Int])

case class DrugReference(ids: Seq[String],
                         source: String,
                         urls: Seq[String])

case class MechanismOfActionRow(mechanismOfAction: String,
                                targetName: String,
                                targets: Option[Seq[String]],
                                references: Option[Seq[DrugReference]])

case class LinkedDiseases(count: Int, rows: Seq[String])
case class LinkedTargets(count: Int, rows: Seq[String])

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
                mechanismsOfAction: MechanismsOfAction,
                linkedDiseases: LinkedDiseases,
                linkedTargets: LinkedTargets)

object Drug {
  object JSONImplicits {
    implicit val linkedDiseasesImpW = Json.format[models.entities.LinkedDiseases]
    implicit val linkedTargetsImpW = Json.format[models.entities.LinkedTargets]
    implicit val withdrawnNoticeImpW = Json.format[models.entities.WithdrawnNotice]
    implicit val drugReferenceImpW = Json.format[models.entities.DrugReference]
    implicit val mechanismOfActionRowImpW = Json.format[models.entities.MechanismOfActionRow]
    implicit val mechanismOfActionImpW = Json.format[models.entities.MechanismsOfAction]
    implicit val drugImpW = Json.format[models.entities.Drug]
  }

  def fromJsValue(jObj: JsValue): Option[Drug] = {
    /* apply transformers for json and fill the target
     start from internal objects and then map the external
     */
    import Drug.JSONImplicits._
    val source = (__ \ '_source).json.pick
    jObj.transform(source).asOpt.map(obj => {
//      println(Json.prettyPrint(obj))
      obj.as[models.entities.Drug]
    })
  }
}
