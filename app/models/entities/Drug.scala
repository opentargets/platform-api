package models.entities

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

case class DrugWarningReference(id: String, source: String, url: String)
case class DrugWarning(toxicityClass: Option[String],
                       country: Option[String],
                       description: Option[String],
                       references: Option[Seq[DrugWarningReference]],
                       warningType: String,
                       year: Option[Int],
                       meddraSocCode: Option[Int])

case class Reference(ids: Option[Seq[String]], source: String, urls: Option[Seq[String]])

case class IndicationReference(ids: Option[Seq[String]], source: String)

case class MechanismOfActionRow(mechanismOfAction: String,
                                actionType: Option[String],
                                targetName: Option[String],
                                targets: Option[Seq[String]],
                                references: Option[Seq[Reference]])

case class IndicationRow(maxPhaseForIndication: Long,
                         disease: String,
                         references: Option[Seq[IndicationReference]])

case class LinkedIds(count: Int, rows: Seq[String])

case class Indications(id: String, indications: Seq[IndicationRow], indicationCount: Long, approvedIndications: Option[Seq[String]])

case class MechanismsOfAction(rows: Seq[MechanismOfActionRow],
                              uniqueActionTypes: Seq[String],
                              uniqueTargetTypes: Seq[String])

case class MechanismOfActionRaw(chemblIds: Seq[String],
                                targets: Option[Seq[String]],
                                mechanismOfAction: String,
                                actionType: Option[String],
                                targetType: Option[String],
                                targetName: Option[String],
                                references: Option[Seq[Reference]])
case class DrugReferences(source: String, reference: Seq[String])

case class Drug(id: String,
                name: String,
                synonyms: Seq[String],
                tradeNames: Seq[String],
                childChemblIds: Option[Seq[String]],
                yearOfFirstApproval: Option[Int],
                drugType: String,
                isApproved: Option[Boolean],
                crossReferences: Option[Seq[DrugReferences]],
                parentId: Option[String],
                maximumClinicalTrialPhase: Option[Int],
                hasBeenWithdrawn: Boolean,
                linkedDiseases: Option[LinkedIds],
                linkedTargets: Option[LinkedIds],
                blackBoxWarning: Boolean,
                description: Option[String])

object Drug {
  implicit val linkedIdsImpW = Json.format[models.entities.LinkedIds]
//  implicit val drugWarningRefenceImpW = Json.format[models.entities.DrugWarningReference]
  implicit val drugWarningsReferenceImpR: Reads[models.entities.DrugWarningReference] = (
    (JsPath \ "ref_id").read[String] and
      (JsPath \ "ref_type").read[String] and
      (JsPath \ "ref_url").read[String]
    )(DrugWarningReference.apply _)
  implicit val drugWarningReferenceImpW: OWrites[DrugWarningReference] = Json.writes[DrugWarningReference]
  implicit val drugWarningImpW = Json.format[models.entities.DrugWarning]
  implicit val referenceImpW = Json.format[models.entities.Reference]
  implicit val mechanismOfActionRowImpW = Json.format[models.entities.MechanismOfActionRow]
  implicit val mechanismOfActionImpW = Json.format[models.entities.MechanismsOfAction]
  implicit val indicationReferenceImpW = Json.format[models.entities.IndicationReference]
  implicit val indicationRowImpW = Json.format[models.entities.IndicationRow]
  implicit val indicationsImpW = Json.format[models.entities.Indications]
  implicit val mechanismOfActionRaw = Json.format[models.entities.MechanismOfActionRaw]

  def mechanismOfActionRaw2MechanismOfAction(raw: Seq[MechanismOfActionRaw]): MechanismsOfAction = {
    val rows =
      raw.map(
        r =>
          MechanismOfActionRow(r.mechanismOfAction,
                               r.actionType,
                               r.targetName,
                               r.targets,
                               r.references))
    val utt = raw.flatMap(_.targetType).distinct
    val uat = raw.flatMap(_.actionType).distinct
    MechanismsOfAction(rows, uat, utt)
  }

  implicit val DrugXRefImpF = Json.format[models.entities.DrugReferences]

  private val drugTransformerXRef: Reads[JsObject] = __.json.update(
    /*
    The incoming Json has an cross reference object with an array for each source. We don't know in advance which drug
    has which references, so we need to flatten the object into an array of objects for conversion into case classes.
    See: https://www.playframework.com/documentation/2.6.x/ScalaJsonTransformers
     */
    __.read[JsObject]
      .map { o =>
        {
          if (o.keys.contains("crossReferences")) {
            val cr: Seq[(String, JsValue)] = o.value("crossReferences").as[JsObject].fields
            val newJsonObjects: Seq[JsObject] =
              cr.map(xref => JsObject(Seq("source" -> JsString(xref._1), "reference" -> xref._2)))
            (o - "crossReferences") ++ Json.obj("crossReferences" -> newJsonObjects)
          } else {
            o
          }
        }
      }
  )
  implicit val drugImplicitR: Reads[Drug] = drugTransformerXRef.andThen(Json.reads[Drug])
  implicit val drugImplicitW: OWrites[Drug] = Json.writes[Drug]
}
