package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AggregationFilter(name: String, path: Seq[String])
case class AggregationSettings(filters: Seq[AggregationFilter])
object AggregationSettings {
  val empty = AggregationSettings(Seq.empty)
}
case class AggregationMapping(key: String, pathKeys: IndexedSeq[String], nested: Boolean)

case class Aggregation(key: String, uniques: Long, aggs: Option[Seq[Aggregation]])
case class NamedAggregation(name: String, uniques: Option[Long], rows: Seq[Aggregation])
case class Aggregations(uniques: Long, aggs: Seq[NamedAggregation])

object Aggregations {
  val logger = Logger(this.getClass)
  val empty = Aggregations(0, Seq.empty)

  // Reads.seq[User](userReads)
  object JSONImplicits {
    implicit val aggregationImpWrites = Json.writes[Aggregation]
    implicit val aggregationImpReads: Reads[Aggregation] =
      ((__ \ "key").read[String] and
        (__ \ "uniques" \\ "value").readWithDefault[Long](0) and
        (__ \ "aggs" \\ "buckets").lazyReadNullable(Reads.seq[Aggregation](aggregationImpReads))
        )(Aggregation.apply _)

    implicit val namedAggregationImpFormat = Json.format[NamedAggregation]
    implicit val aggregationsImpFormat = Json.writes[Aggregations]

    implicit val aggregationFilterImpFormat = Json.format[AggregationFilter]
    implicit val aggregationSettingsImpFormat = Json.format[AggregationSettings]
  }
}
