package models.entities

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import slick.jdbc.GetResult

case class DrugReferences(source: String, ids: Seq[String])

case class Drug(
    id: String,
    name: String,
    synonyms: Seq[String],
    tradeNames: Seq[String],
    childChemblIds: Option[Seq[String]], // Gone?
    drugType: String,
    isApproved: Option[Boolean],
    crossReferences: Option[Seq[DrugReferences]],
    parentId: Option[String],
    maximumClinicalTrialPhase: Option[Double],
    hasBeenWithdrawn: Boolean,
    blackBoxWarning: Boolean,
    description: Option[String]
)

object Drug {
  implicit val getResult: GetResult[Drug] = GetResult(r => Json.parse(r.<<[String]).as[Drug])
  implicit val DrugXRefImpW: OFormat[DrugReferences] = Json.format[DrugReferences]
  implicit val drugImplicitR: Reads[Drug] = Json.reads[Drug]
  implicit val drugImplicitW: OWrites[Drug] = Json.writes[Drug]
}
