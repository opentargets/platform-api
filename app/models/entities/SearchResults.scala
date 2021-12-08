package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class SearchResultAggCategory(name: String, total: Long)

case class SearchResultAggEntity(
                                  name: String,
                                  total: Long,
                                  categories: Seq[SearchResultAggCategory]
                                )

case class SearchResultAggs(total: Long, entities: Seq[SearchResultAggEntity])

case class SearchResult(
                         id: String,
                         entity: String,
                         category: Seq[String],
                         name: String,
                         description: Option[String],
                         keywords: Option[Seq[String]],
                         multiplier: Double,
                         prefixes: Option[Seq[String]],
                         ngrams: Option[Seq[String]],
                         score: Double,
                         highlights: Seq[String]
                       )

case class SearchResults(
                          hits: Seq[SearchResult],
                          aggregations: Option[SearchResultAggs],
                          total: Long
                        )

object SearchResults {
  val empty: SearchResults = SearchResults(Seq.empty, None, 0)
  implicit val searchResultAggsCategoryImpW: OWrites[SearchResultAggCategory] =
    Json.writes[models.entities.SearchResultAggCategory]
  implicit val searchResultAggsEntityImpW: OWrites[SearchResultAggEntity] =
    Json.writes[models.entities.SearchResultAggEntity]
  implicit val searchResultAggsImpW: OWrites[SearchResultAggs] =
    Json.writes[models.entities.SearchResultAggs]

  implicit val searchResultAggCategoryImpR: Reads[models.entities.SearchResultAggCategory] = (
    (__ \ "key").read[String] and
      (__ \ "doc_count").read[Long]
    ) (SearchResultAggCategory.apply _)

  implicit val searchResultAggEntityImpR: Reads[models.entities.SearchResultAggEntity] = (
    (__ \ "key").read[String] and
      (__ \ "doc_count").read[Long] and
      (__ \ "categories" \ "buckets").read[Seq[models.entities.SearchResultAggCategory]]
  )(SearchResultAggEntity.apply _)

  implicit val searchResultAggsImpR: Reads[models.entities.SearchResultAggs] =
    ((__ \ "total" \ "value").readWithDefault[Long](0) and
      (__ \ "entities" \ "buckets")
        .read[Seq[models.entities.SearchResultAggEntity]])(models.entities.SearchResultAggs.apply _)

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
      }) (SearchResult.apply _)

  implicit val msearchResultsImpW: OFormat[SearchResults] =
    Json.format[models.entities.SearchResults]
}
