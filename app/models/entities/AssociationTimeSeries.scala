package models.entities

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult
import utils.db.DbJsonParser.fromPositionedResult

enum AggregationType:
  case Overall, DatasourceId

case class AssociationTimeSeries(
    diseaseId: String,
    targetId: String,
    aggregationType: AggregationType,
    aggregationValue: String,
    year: Option[Int],
    associationScore: Double,
    novelty: Option[Double],
    yearlyEvidenceCount: Option[Int],
    isDirect: Boolean,
    meta_total: Long
)

case class AssociationTimeSeriesResults(
    count: Long,
    rows: Vector[AssociationTimeSeries]
)

object AssociationTimeSeriesResults {
  val empty: AssociationTimeSeriesResults = AssociationTimeSeriesResults(0, Vector.empty)
  implicit val getAssociationTimeSeriesRowFromDB: GetResult[AssociationTimeSeries] =
    GetResult(fromPositionedResult[AssociationTimeSeries])
  implicit val AssociationTimeSeriesImp: OFormat[AssociationTimeSeries] =
    Json.format[AssociationTimeSeries]
  implicit val AssociationTimeSeriesResultsImp: OFormat[AssociationTimeSeriesResults] =
    Json.format[AssociationTimeSeriesResults]
  implicit val AggregationTypeFormat: OFormat[AggregationType] =
    Json.format[AggregationType]
}
