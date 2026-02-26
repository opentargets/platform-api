package models.entities

import utils.OTLogging
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import slick.jdbc.GetResult

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

case class DrugWarnings(
    chemblId: String,
    drugWarnings: Seq[DrugWarning]
)

object DrugWarning extends OTLogging {
  implicit val getDrugWarningsFromDB: GetResult[DrugWarnings] =
    GetResult(r => Json.parse(r.<<[String]).as[DrugWarnings])
  implicit val drugWarningsImpF: OFormat[DrugWarnings] = Json.format[models.entities.DrugWarnings]
  implicit val drugWarningsReferenceImpF: OFormat[models.entities.DrugWarningReference] =
    Json.format[models.entities.DrugWarningReference]
  implicit val drugWarningImpF: OFormat[models.entities.DrugWarning] =
    Json.format[models.entities.DrugWarning]
}
