package models.entities

import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.Reads._

case class DiseaseSynonyms(relation: String, terms: Seq[String])

case class DiseaseOntology(isTherapeuticArea: Boolean)

case class Disease(id: String,
                   name: String,
                   therapeuticAreas: Seq[String],
                   description: Option[String],
                   dbXRefs: Option[Seq[String]],
                   directLocationIds: Option[Seq[String]],
                   indirectLocationIds: Option[Seq[String]],
                   obsoleteTerms: Option[Seq[String]],
                   synonyms: Option[Seq[DiseaseSynonyms]],
                   parents: Seq[String],
                   children: Seq[String],
                   ancestors: Seq[String],
                   descendants: Seq[String],
                   ontology: DiseaseOntology)

object Disease extends Logging {

  implicit val DiseaseOntologyImpF = Json.format[models.entities.DiseaseOntology]
  implicit val DiseaseSynonymsImpF = Json.format[models.entities.DiseaseSynonyms]

  private val diseaseTransformerSynonyms: Reads[JsObject] = __.json.update(
    /*
    The incoming Json has an synonyms object with an array for each relation. We don't know in advance which disease
    has which terms, so we need to flatten the object into an array of objects for conversion into case classes.
    See: https://www.playframework.com/documentation/2.6.x/ScalaJsonTransformers
     */
    __.read[JsObject]
      .map { o =>
        {
          if (o.fields.map(_._1).contains("synonyms")) {
            val cr: Seq[(String, JsValue)] = o.value("synonyms").as[JsObject].fields
            val newJsonObjects: Seq[JsObject] =
              cr.map(xref => JsObject(Seq("relation" -> JsString(xref._1), "terms" -> xref._2)))
            (o - "synonyms") ++ Json.obj("synonyms" -> newJsonObjects)
          } else {
            o
          }
        }
      }
  )
  implicit val diseaseImpR: Reads[Disease] = diseaseTransformerSynonyms.andThen(Json.reads[Disease])
  implicit val diseaseImpW: OWrites[Disease] = Json.writes[Disease]
}
