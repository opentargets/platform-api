package models.entities

import com.sksamuel.elastic4s.requests.searches.SearchHit
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class SearchResultAggCategory(name: String, total: Long)

case class SearchResultAggEntity(name: String, total: Long, categories: Seq[SearchResultAggCategory])

case class SearchResultAggs(total: Long, entities: Seq[SearchResultAggEntity])

case class SearchResult(id: String, entity: String, category: Seq[String], name: String,
                        description: Option[String], keywords: Option[Seq[String]], multiplier: Double,
                        prefixes: Option[Seq[String]], ngrams: Option[Seq[String]],
                        terms: Option[Seq[String]])

case class SearchResults(total: Long, results: Seq[SearchResult], aggregations: Option[SearchResultAggs])

object SearchResult {
  object JSONImplicits {
    implicit val searchResultAggsCategoryImpW = Json.writes[models.entities.SearchResultAggCategory]
    implicit val searchResultAggsEntityImpW = Json.writes[models.entities.SearchResultAggEntity]
    implicit val searchResultAggsImpW = Json.writes[models.entities.SearchResultAggs]

    implicit val searchResultAggCategoryImpR: Reads[models.entities.SearchResultAggCategory] = (
      (JsPath \ "key").read[String] and
        (JsPath \ "doc_count").read[Long]
    )(SearchResultAggCategory.apply _)

    implicit val searchResultAggEntityImpR: Reads[models.entities.SearchResultAggEntity] = (
      (JsPath \ "key").read[String] and
        (JsPath \ "doc_count").read[Long] and
        (JsPath \ "categories" \ "buckets").read[Seq[models.entities.SearchResultAggCategory]]
    )(SearchResultAggEntity.apply _)

    implicit val searchResultAggsImpR: Reads[models.entities.SearchResultAggs] =
      ((JsPath \ "total" \ "value").readWithDefault[Long](0) and
        (JsPath \ "entities" \ "buckets").read[Seq[models.entities.SearchResultAggEntity]]
        )(models.entities.SearchResultAggs.apply _)

    implicit val searchResultImpW = Json.format[models.entities.SearchResult]
    implicit val searchResultsImpW = Json.format[models.entities.SearchResults]
  }

  def fromJsValue(jObj: JsValue): Option[SearchResult] = {
    /* apply transformers for json and fill the searchresult
     start from internal objects and then map the external
     */
    import SearchResult.JSONImplicits._
    val source = (__ \ '_source).json.pick
    jObj.transform(source).asOpt.map(obj => {
      obj.as[SearchResult]
    })
  }
}
