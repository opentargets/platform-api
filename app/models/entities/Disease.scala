package models.entities

import utils.OTLogging
import play.api.libs.json.*
import slick.jdbc.GetResult
import utils.db.DbJsonParser.fromPositionedResult

case class DiseaseSynonyms(relation: String, terms: Seq[String])

case class Disease(
    id: String,
    name: String,
    therapeuticAreas: Seq[String],
    description: Option[String],
    dbXRefs: Seq[String],
    directLocationIds: Option[Seq[String]],
    indirectLocationIds: Option[Seq[String]],
    obsoleteTerms: Seq[String],
    synonyms: Option[Seq[DiseaseSynonyms]],
    parents: Seq[String],
    children: Seq[String],
    ancestors: Seq[String],
    descendants: Seq[String],
    isTherapeuticArea: Boolean
)

object Disease extends OTLogging {
  implicit val getDiseaseFromDB: GetResult[Disease] =
    GetResult(fromPositionedResult[Disease])
  implicit val DiseaseSynonymsImpF: OFormat[DiseaseSynonyms] =
    Json.format[models.entities.DiseaseSynonyms]
  implicit val diseaseImpF: OFormat[Disease] = Json.format[Disease]
}
