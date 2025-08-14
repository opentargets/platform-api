package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import play.api.Logging

case class IntervalsQuery(chromosome: String,
                          start: Int,
                          end: Int,
                          tableName: String,
                          offset: Int,
                          size: Int
) extends Queryable
    with Logging {

  private val positionalQuery = Where(
    Functions.and(
      Functions.equals(column("chromosome"), literal(chromosome)),
      Functions.lessOrEquals(column("start"), literal(start)),
      Functions.greaterOrEquals(column("end"), literal(end))
    )
  )

  val totals: Query =
    Query(
      Select(Functions.count(Column.star) :: Nil),
      From(column(tableName)),
      positionalQuery
    )

  override val query: Query =
    Query(
      Select(
        Column.star :: Nil
      ),
      From(column(tableName)),
      positionalQuery,
      OrderBy(column("chromosome") :: column("start") :: column("end") :: Nil),
      Limit(offset, size),
      Format("JSONEachRow")
    )
}
