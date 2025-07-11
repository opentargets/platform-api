package models.entities
import slick.jdbc.GetResult
import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.Reads._
import clickhouse.rep.SeqRep._

// case class DiseaseSynonyms(relation: String, terms: Seq[String])
case class DiseaseSynonyms(
    hasExactSynonym: Option[Seq[String]],
    hasRelatedSynonym: Option[Seq[String]],
    hasNarrowSynonym: Option[Seq[String]],
    hasBroadSynonym: Option[Seq[String]]
)
case class DiseaseOntology(isTherapeuticArea: Boolean)

case class Disease(
    id: String,
    code: String,
    name: String,
    description: Option[String],
    dbXRefs: Option[Seq[String]],
    parents: Seq[String],
    synonyms: Option[DiseaseSynonyms],
    obsoleteTerms: Option[Seq[String]],
    obsoleteXRefs: Option[Seq[String]],
    children: Seq[String],
    ancestors: Seq[String],
    therapeuticAreas: Seq[String],
    // directLocationIds: Option[Seq[String]] = None,
    // indirectLocationIds: Option[Seq[String]] = None,
    descendants: Seq[String],
    ontology: DiseaseOntology
)

object Disease extends Logging {
  implicit val getDiseaseRowFromDB: GetResult[Disease] =
    GetResult { r =>
      val id: String = r.<<
      val code: String = r.<<
      val name: String = r.<<
      val description: Option[String] = r.<<? // description
      val dbXRefs = if (r.wasNull()) None else Some(StrSeqRep(r.<<).rep) // dbXRefs
      val parents = StrSeqRep(r.<<).rep // parents
      val synonyms = if (r.wasNull()) None else Some(getDiseaseSynonymsRowFromDB(r)) // synonyms
      val obsoleteTerms = if (r.wasNull()) None else Some(StrSeqRep(r.<<).rep) // obsoleteTerms
      val obsoleteXRefs = if (r.wasNull()) None else Some(StrSeqRep(r.<<).rep) // obsoleteXRefs
      val children = StrSeqRep(r.<<).rep // children
      val ancestors = StrSeqRep(r.<<).rep // ancestors
      val therapeuticAreas = StrSeqRep(r.<<).rep // therapeuticAreas
      val descendants = StrSeqRep(r.<<).rep // descendants
      val ontology = getDiseaseOntologyRowFromDB(r) // ontology
      Disease(
        id,
        code,
        name,
        description,
        dbXRefs,
        parents,
        synonyms,
        obsoleteTerms,
        obsoleteXRefs,
        children,
        ancestors,
        therapeuticAreas,
        descendants,
        ontology
      )
    }

  implicit val getDiseaseSynonymsRowFromDB: GetResult[DiseaseSynonyms] =
    GetResult(r =>
      DiseaseSynonyms(
        if (r.wasNull()) None else Some(StrSeqRep(r.<<).rep), // hasExactSynonym
        if (r.wasNull()) None else Some(StrSeqRep(r.<<).rep), // hasRelatedSynonym
        if (r.wasNull()) None else Some(StrSeqRep(r.<<).rep), // hasNarrowSynonym
        if (r.wasNull()) None else Some(StrSeqRep(r.<<).rep) // hasBroadSynonym
      )
    )
  implicit val getDiseaseOntologyRowFromDB: GetResult[DiseaseOntology] =
    GetResult { r =>
      val isTherapeuticArea: Boolean = r.<<
      DiseaseOntology(
        isTherapeuticArea = isTherapeuticArea
      )
    }
  implicit val DiseaseOntologyImpF: OFormat[DiseaseOntology] =
    Json.format[models.entities.DiseaseOntology]
  implicit val DiseaseSynonymsImpF: OFormat[DiseaseSynonyms] =
    Json.format[models.entities.DiseaseSynonyms]

  private val diseaseTransformerSynonyms: Reads[JsObject] = __.json.update(
    /*
    The incoming Json has an synonyms object with an array for each relation. We don't know in advance which disease
    has which terms, so we need to flatten the object into an array of objects for conversion into case classes.
    See: https://www.playframework.com/documentation/2.6.x/ScalaJsonTransformers
     */
    __.read[JsObject]
      .map { o =>
        if (o.fields.map(_._1).contains("synonyms")) {
          val cr: Seq[(String, JsValue)] = o.value("synonyms").as[JsObject].fields.to(Seq)
          val newJsonObjects: Seq[JsObject] =
            cr.map(xref => JsObject(Seq("relation" -> JsString(xref._1), "terms" -> xref._2)))
          (o - "synonyms") ++ Json.obj("synonyms" -> newJsonObjects)
        } else {
          o
        }
      }
  )
  implicit val diseaseImpR: Reads[Disease] = diseaseTransformerSynonyms.andThen(Json.reads[Disease])
  implicit val diseaseImpW: OWrites[Disease] = Json.writes[Disease]
}
