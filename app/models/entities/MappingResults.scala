package models.entities

import org.slf4j.{Logger, LoggerFactory}
import play.api.Logging
import play.api.libs.json.*

case class MappingResult(
    term: String,
    hits: Option[Seq[SearchResult]]
)

case class MappingResults(
    mappings: Seq[MappingResult],
    aggregations: Option[SearchResultAggs],
    total: Long
)

object MappingResults {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

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
