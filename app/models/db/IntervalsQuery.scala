package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import play.api.Logging
import com.sksamuel.elastic4s.requests.common.Operator.Or

case class IntervalsQuery(chromosome: String,
                          start: Int,
                          end: Int,
                          tableName: String,
                          offset: Int,
                          size: Int
) extends Queryable
    with Logging {

  private val positional_query = Where(
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
      positional_query
    )

  override val query: Query =
    Query(
      Select(
        column("chromosome") ::
          column("start") ::
          column("end") ::
          column("geneId") ::
          column("biosampleName") ::
          column("intervalType") ::
          column("score") ::
          column("resourceScore") ::
          column("datasourceId") ::
          column("pmid") :: Nil
      ),
      From(column(tableName)),
      positional_query,
      OrderBy(column("chromosome") :: column("start") :: column("end") :: Nil),
      Limit(offset, size)
    )
}
