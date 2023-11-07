package models.entities

import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class MappingResult(
    term: String,
    hits: Option[Seq[SearchResult]]
)

case class MappingResults(
    mappings: Seq[MappingResult],
    aggregations: Option[SearchResultAggs],
    total: Long
)

object MappingResults extends Logging {
  val empty: MappingResults = MappingResults(Seq.empty, None, 0)
  implicit val searchResultAggsCategoryImpW: OWrites[SearchResultAggCategory] =
    Json.writes[models.entities.SearchResultAggCategory]
  implicit val searchResultAggsEntityImpW: OWrites[SearchResultAggEntity] =
    Json.writes[models.entities.SearchResultAggEntity]
  implicit val searchResultAggsImpW: OWrites[SearchResultAggs] =
    Json.writes[models.entities.SearchResultAggs]
  implicit val searchResultImpW: OWrites[SearchResult] = Json.writes[models.entities.SearchResult]

  implicit val searchResultImpR: Reads[models.entities.SearchResult] =
    ((__ \ "_source" \ "id").read[String] and
      (__ \ "_source" \ "entity").read[String] and
      (__ \ "_source" \ "category").read[Seq[String]] and
      (__ \ "_source" \ "name").read[String] and
      (__ \ "_source" \ "description").readNullable[String] and
      (__ \ "_source" \ "keywords").readNullable[Seq[String]] and
      (__ \ "_source" \ "multiplier").read[Double] and
      (__ \ "_source" \ "prefixes").readNullable[Seq[String]] and
      (__ \ "_source" \ "ngrams").readNullable[Seq[String]] and
      (__ \ "_score").read[Double] and
      (__ \ "highlight").readNullable[Map[String, Seq[String]]].map {
        case Some(m) =>
          (for {
            s <- m.flatMap(_._2)
          } yield s).toSeq.distinct
        case None => Seq.empty[String]
      })(SearchResult.apply _)

  implicit val mappingResultImpW: OWrites[MappingResult] =
    Json.writes[models.entities.MappingResult]

  implicit val mappingResultsW: OWrites[MappingResults] = Json.writes[MappingResults]
}
