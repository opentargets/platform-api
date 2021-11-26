package models.entities

import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AggregationFilter(name: String, path: Seq[String])

case class AggregationMapping(key: String, pathKeys: IndexedSeq[String], nested: Boolean)

case class Aggregation(key: String, uniques: Long, aggs: Option[Seq[Aggregation]])

case class NamedAggregation(name: String, uniques: Option[Long], rows: Seq[Aggregation])

case class Aggregations(uniques: Long, aggs: Seq[NamedAggregation])

object Aggregations extends Logging {
  val empty: Aggregations = Aggregations(0, Seq.empty)

  implicit val aggregationImpWrites: OWrites[Aggregation] = Json.writes[Aggregation]
  implicit val aggregationImpReads: Reads[Aggregation] =
    ((__ \ "key").read[String] and
      (__ \ "uniques" \\ "value").readWithDefault[Long](0) and
      (__ \ "aggs" \\ "buckets")
        .lazyReadNullable(Reads.seq[Aggregation](aggregationImpReads))) (Aggregation.apply _)

  implicit val namedAggregationImpFormat: OFormat[NamedAggregation] = Json.format[NamedAggregation]
  implicit val aggregationsImpFormat: OWrites[Aggregations] = Json.writes[Aggregations]

  implicit val aggregationFilterImpFormat: OFormat[AggregationFilter] = Json.format[AggregationFilter]
}
