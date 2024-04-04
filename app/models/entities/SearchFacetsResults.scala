package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class SearchFacetsResultAggCategory(name: String, total: Long)

case class SearchFacetsResultAggEntity(
    name: String,
    total: Long,
    categories: Seq[SearchFacetsResultAggCategory]
)

case class SearchFacetsResultAggs(total: Long, entities: Seq[SearchFacetsResultAggEntity])

case class SearchFacetsResult(
    id: String,
    label: String,
    category: String,
    entityIds: Option[Seq[String]],
    facetIds: Option[Seq[String]],
    score: Double,
    highlights: Seq[String]
)

case class SearchFacetsResults(
    hits: Seq[SearchFacetsResult],
    total: Long
)

object SearchFacetsResults {
  val empty: SearchFacetsResults = SearchFacetsResults(Seq.empty, 0)

  implicit val SearchFacetsResultImpW: OWrites[SearchFacetsResult] =
    Json.writes[models.entities.SearchFacetsResult]

  implicit val SearchFacetsResultImpR: Reads[models.entities.SearchFacetsResult] =
    ((__ \ "_id").read[String] and
      (__ \ "_source" \ "label").read[String] and
      (__ \ "_source" \ "category").read[String] and
      (__ \ "_source" \ "entityIds").readNullable[Seq[String]] and
      (__ \ "_source" \ "facetIds").readNullable[Seq[String]] and
      (__ \ "_score").read[Double] and
      (__ \ "highlight").readNullable[Map[String, Seq[String]]].map {
        case Some(m) =>
          (for {
            s <- m.flatMap(_._2)
          } yield s).toSeq.distinct
        case None => Seq.empty[String]
      })(SearchFacetsResult.apply _)

  implicit val mSearchFacetsResultsImpW: OFormat[SearchFacetsResults] =
    Json.format[models.entities.SearchFacetsResults]
}
