package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import play.api.Logging

case class BaselineExpressionQuery(targetId: String, tableName: String, offset: Int, size: Int)
    extends Queryable
    with Logging {

  private val positional_query = Where(
    Functions.equals(column("targetId"), literal(targetId))
  )

  val totals: Query =
    Query(
      Select(Functions.count(Column.star) :: Nil),
      From(column(tableName)),
      positional_query
    )

  override val query: Query =
    Query(
      Select(
        Column.star :: Nil
      ),
      From(column(tableName)),
      positional_query,
      OrderBy(column("targetId") :: Nil),
      Limit(offset, size)
    )
}
