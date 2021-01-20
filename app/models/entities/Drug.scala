package models.entities

import play.api.libs.json._

case class WithdrawnNotice(classes: Option[Seq[String]],
                           countries: Option[Seq[String]],
                           reasons: Option[Seq[String]],
                           year: Option[Int])

case class Reference(ids: Option[Seq[String]], source: String, urls: Option[Seq[String]])

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
                withdrawnNotice: Option[WithdrawnNotice],
                approvedIndications: Option[Seq[String]],
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
  implicit val DrugXRefImpF = Json.format[models.entities.DrugReferences]

  private val drugTransformerXRef: Reads[JsObject] = __.json.update(
    /*
    The incoming Json has an cross reference object with an array for each source. We don't know in advance which drug
    has which references, so we need to flatten the object into an array of objects for conversion into case classes.
    See: https://www.playframework.com/documentation/2.6.x/ScalaJsonTransformers
     */
    __.read[JsObject]
      .map { o => {
        if (o.fields.map(_._1).contains("crossReferences")) {
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
