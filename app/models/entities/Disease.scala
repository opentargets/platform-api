package models.entities

import play.api.{Logger, Logging}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/** this is a temporal HACK while we get all phenetypes into the slim efo ontology for OT.
 * It should later resolve as a normal disease entity */
case class Phenotype(url: String, name: String, disease: String)

case class Disease(id: String,
                   name: String,
                   therapeuticAreas: Seq[String],
                   description: Option[String],
                   synonyms: Seq[String],
                   parents: Seq[String],
                   children: Seq[String],
                   ancestors: Seq[String],
                   descendants: Seq[String],
                   phenotypes: Seq[Phenotype],
                   isTherapeuticArea: Boolean
                  )

object Disease extends Logging {
  implicit val phenotypeImpW = Json.writes[Phenotype]
  implicit val phenotypeImpR: Reads[Phenotype] =
    ((__ \ "url").read[String] and
      (__ \ "name").read[String] and
      (__ \ "disease").read[String]
      ) (Phenotype.apply _)

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
      (__ \ "phenotypes" \ "rows").readWithDefault[Seq[Phenotype]](Seq.empty) and
      (__ \ "ontology" \ "isTherapeuticArea").read[Boolean]
    ) (Disease.apply _)
}
