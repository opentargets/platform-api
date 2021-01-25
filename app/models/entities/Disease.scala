package models.entities

import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Disease(id: String,
                   name: String,
                   therapeuticAreas: Seq[String],
                   description: Option[String],
                   synonyms: Seq[String],
                   parents: Seq[String],
                   children: Seq[String],
                   ancestors: Seq[String],
                   descendants: Seq[String],
                   isTherapeuticArea: Boolean)

object Disease extends Logging {

  implicit val diseaseImpW = Json.writes[Disease]
  implicit val diseaseImpR: Reads[Disease] = (
    (__ \ "id").read[String] and
      (__ \ "name").read[String] and
      (__ \ "therapeuticAreas").read[Seq[String]] and
      (__ \ "description").readNullable[String] and
      (__ \ "synonyms").read[Seq[String]] and
      (__ \ "parents").read[Seq[String]] and
      (__ \ "children").read[Seq[String]] and
      (__ \ "ancestors").read[Seq[String]] and
      (__ \ "descendants").read[Seq[String]] and
      (__ \ "ontology" \ "isTherapeuticArea").read[Boolean]
  )(Disease.apply _)
}
