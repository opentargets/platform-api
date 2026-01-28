package models.entities

import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.Reads._
import slick.jdbc.GetResult

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

object Disease extends Logging {
  implicit val getDiseaseFromDB: GetResult[Disease] =
    GetResult(r => Json.parse(r.<<[String]).as[Disease])
  implicit val DiseaseSynonymsImpF: OFormat[DiseaseSynonyms] =
    Json.format[models.entities.DiseaseSynonyms]
  implicit val diseaseImpF: OFormat[Disease] = Json.format[Disease]
}
