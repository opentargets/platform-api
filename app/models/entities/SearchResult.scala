package models.entities

import com.sksamuel.elastic4s.requests.searches.SearchHit
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class SearchResultAggCategory(name: String, total: Long)

case class SearchResultAggEntity(name: String, total: Long, categories: Seq[SearchResultAggCategory])

case class SearchResultAggs(total: Long, entities: Seq[SearchResultAggEntity])

case class SearchResult(id: String, entity: String, category: Seq[String], name: String,
                        description: Option[String], keywords: Option[Seq[String]], multiplier: Double,
                        prefixes: Option[Seq[String]], ngrams: Option[Seq[String]],
                        score: Double, highlights: Seq[String])

case class SearchResults(hits: Seq[SearchResult],
                         aggregations: Option[SearchResultAggs], total: Long)

object SearchResults {
  val empty = SearchResults(Seq.empty, None, 0)
}

object SearchResult {
  val logger = Logger(this.getClass)
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

    implicit val searchResultImpW = Json.writes[models.entities.SearchResult]

    implicit val searchResultImpR: Reads[models.entities.SearchResult] =
      ((JsPath \ "_source" \ "id").read[String] and
        (JsPath \ "_source" \ "entity").read[String] and
        (JsPath \ "_source" \ "category").read[Seq[String]] and
        (JsPath \ "_source" \ "name").read[String] and
        (JsPath \ "_source" \ "description").readNullable[String] and
        (JsPath \ "_source" \ "keywords").readNullable[Seq[String]] and
        (JsPath \ "_source" \ "multiplier").read[Double] and
        (JsPath \ "_source" \ "prefixes").readNullable[Seq[String]] and
        (JsPath \ "_source" \ "ngrams").readNullable[Seq[String]] and
        (JsPath \ "_score").read[Double] and
        (JsPath \ "highlight").readNullable[Map[String, Seq[String]]].map {
          case Some(m) =>
            (for {
              s <- m.flatMap(_._2)
            } yield s).toSeq.distinct
          case None => Seq.empty[String]
        }
        )(SearchResult.apply _)

    implicit val msearchResultsImpW = Json.format[models.entities.SearchResults]
  }
}
