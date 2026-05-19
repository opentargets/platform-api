package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele.*
import utils.OTLogging
import models.gql.AggregationTypeEnum

case class AssociationTimeSeriesQuery(
    diseaseId: String,
    targetId: String,
    isDirect: Boolean,
    tableName: String,
    offset: Int,
    size: Int,
    aggregationTypeInclude: Option[Seq[AggregationTypeEnum.Value]] = None,
    yearFrom: Option[Int] = None,
    yearTo: Option[Int] = None
) extends Queryable
    with OTLogging {

  private val aggregationTypes: Seq[AggregationTypeEnum.Value] = aggregationTypeInclude.getOrElse(
    Seq(AggregationTypeEnum.overall, AggregationTypeEnum.datasourceId)
  )

  private val aggregationTypeFilter =
    Functions.in(column("aggregationType"),
                 Functions.set(aggregationTypes.map(t => literal(t.toString)))
    )

  private val yearFromFilter: Option[Column] = yearFrom match {
    case Some(year) => Some(Functions.greaterOrEquals(column("year"), literal(year)))
    case None       => None
  }
  private val yearToFilter: Option[Column] = yearTo match {
    case Some(year) => Some(Functions.lessOrEquals(column("year"), literal(year)))
    case None       => None
  }
  private val yearFilter: Column = (yearFromFilter, yearToFilter) match {
    case (Some(from), Some(to)) => Functions.and(from, to)
    case (Some(filter), None)   => filter
    case (None, Some(filter))   => filter
    case (None, None)           => literal(true)
  }

  private val positionalQuery =
    Functions.and(
      Functions.equals(column("diseaseId"), literal(diseaseId)),
      Functions.equals(column("targetId"), literal(targetId)),
      Functions.equals(column("isDirect"), literal(isDirect))
    )

  private val queryWithFilters = Where(
    Functions.and(
      positionalQuery,
      aggregationTypeFilter,
      yearFilter
    )
  )

  val totals: Query =
    Query(
      Select(Functions.count(Column.star) :: Nil),
      From(column(tableName)),
      queryWithFilters
    )

  override val query: Query =
    Query(
      Select(
        Column.star :: Functions.countOver("meta_total") :: Nil
      ),
      From(column(tableName)),
      queryWithFilters,
      OrderBy(column("year").asc :: Nil),
      Limit(offset, size),
      Format("JSONEachRow")
    )
}
