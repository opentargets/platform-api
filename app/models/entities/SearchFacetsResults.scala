package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class SearchFacetsCategory(name: String, total: Long)

case class Facet(
    label: String,
    category: String,
    entityIds: Option[Seq[String]],
    datasourceId: Option[String]
)

case class SearchFacetsResult(
    id: String,
    label: String,
    category: String,
    entityIds: Option[Seq[String]],
    datasourceId: Option[String],
    score: Double,
    highlights: Seq[String]
)

case class SearchFacetsResults(
    hits: Seq[SearchFacetsResult],
    total: Long,
    categories: Seq[SearchFacetsCategory]
)

object SearchFacetsResults {
  implicit val searchFacetsCategoryImpW: OWrites[SearchFacetsCategory] =
    Json.writes[models.entities.SearchFacetsCategory]

  implicit val facetF: OFormat[Facet] = Json.format[Facet]

  implicit val searchFacetsResultImpW: OWrites[SearchFacetsResult] =
    Json.writes[models.entities.SearchFacetsResult]

  implicit val searchFacetsCategoryImpR: Reads[SearchFacetsCategory] = (
    (__ \ "key").read[String] and
      (__ \ "doc_count").read[Long]
  )(SearchFacetsCategory.apply _)

  implicit val searchFacetsResultImpR: Reads[models.entities.SearchFacetsResult] =
    ((__ \ "_id").read[String] and
      (__ \ "_source" \ "label").read[String] and
      (__ \ "_source" \ "category").read[String] and
      (__ \ "_source" \ "entityIds").readNullable[Seq[String]] and
      (__ \ "_source" \ "datasourceId").readNullable[String] and
      (__ \ "_score").read[Double] and
      (__ \ "highlight").readNullable[Map[String, Seq[String]]].map {
        case Some(m) =>
          (for {
            s <- m.flatMap(_._2)
          } yield s).toSeq.distinct
        case None => Seq.empty[String]
      })(SearchFacetsResult.apply _)

  implicit val searchFacetsResultsImpW: OFormat[SearchFacetsResults] =
    Json.format[models.entities.SearchFacetsResults]
}
