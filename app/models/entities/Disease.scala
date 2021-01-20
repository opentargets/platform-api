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
      (__ \ "synonyms").readWithDefault[Seq[String]](Seq.empty) and
      (__ \ "parents").readWithDefault[Seq[String]](Seq.empty) and
      (__ \ "children").readWithDefault[Seq[String]](Seq.empty) and
      (__ \ "ancestors").readWithDefault[Seq[String]](Seq.empty) and
      (__ \ "descendants").readWithDefault[Seq[String]](Seq.empty) and
      (__ \ "ontology" \ "isTherapeuticArea").read[Boolean]
  )(Disease.apply _)
}
