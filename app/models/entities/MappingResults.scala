package models.entities

import play.api.Logging
import play.api.libs.json._

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
    SearchResults.searchResultAggsCategoryImpW
  implicit val searchResultAggsEntityImpW: OWrites[SearchResultAggEntity] =
    SearchResults.searchResultAggsEntityImpW
  implicit val searchResultAggsImpW: OWrites[SearchResultAggs] = SearchResults.searchResultAggsImpW
  implicit val searchResultImpW: OWrites[SearchResult] = SearchResults.searchResultImpW
  implicit val searchResultImpR: Reads[models.entities.SearchResult] =
    SearchResults.searchResultImpR
  implicit val mappingResultImpW: OWrites[MappingResult] =
    Json.writes[models.entities.MappingResult]
  implicit val mappingResultsW: OWrites[MappingResults] = Json.writes[MappingResults]
}
