package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele.*
import utils.OTLogging

case class NoveltyQuery(diseaseId: String,
                        targetId: String,
                        isDirect: Boolean,
                        tableName: String,
                        offset: Int,
                        size: Int
) extends Queryable
    with OTLogging {

  private val positionalQuery = Where(
    Functions.and(
      Functions.equals(column("diseaseId"), literal(diseaseId)),
      Functions.equals(column("targetId"), literal(targetId)),
      Functions.equals(column("isDirect"), literal(isDirect))
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
      OrderBy(column("year").asc :: Nil),
      Limit(offset, size),
      Format("JSONEachRow")
    )
}
