package models.entities

import com.sksamuel.elastic4s.requests.searches.SearchHit
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class SearchResult(id: String, entity: String, category: Seq[String], name: String,
                        description: Option[String], keywords: Option[Seq[String]], multiplier: Float,
                        prefixes: Option[Seq[String]], ngrams: Option[Seq[String]], terms: Option[Seq[String]])

case class SearchResults(total: Long, results: Seq[SearchResult])

object SearchResult {
  object JSONImplicits {
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
