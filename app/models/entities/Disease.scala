package models.entities

import play.api.Logger
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
                   phenotypes: Seq[String],
                   isTherapeuticArea: Boolean
                  )

object Disease {
  val logger = Logger(this.getClass)

  object JSONImplicits {
    implicit val diseaseImpW = Json.writes[models.entities.Disease]
    implicit val diseaseImpR: Reads[Disease] = (
      (__ \ "id").read[String] and
        (__ \ "name").read[String] and
        (__ \ "therapeuticAreas").read[Seq[String]] and
        (__ \ "description").readNullable[String] and
        (__ \ "synonyms").read[Seq[String]] and
        (__ \ "parents").read[Seq[String]] and
        (__ \ "children").read[Seq[String]] and
        (__ \ "phenotypes" \ "rows").readNullable[Seq[Map[String, String]]]
          .map(_.map( s => s.map(m => m("disease"))).getOrElse(Seq.empty)) and
        (__ \ "ontology" \ "isTherapeuticArea").read[Boolean]
    )(Disease.apply _)
  }
}
