package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele.*
import utils.OTLogging

case class EnhancerToGeneQuery(chromosome: String,
                               start: Int,
                               end: Int,
                               tableName: String,
                               offset: Int,
                               size: Int
) extends Queryable
    with OTLogging {

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
        Column.star :: Functions.countOver("meta_total") :: Nil
      ),
      From(column(tableName)),
      positionalQuery,
      OrderBy(column("score").desc :: Nil),
      Limit(offset, size),
      Format("JSONEachRow")
    )
}
