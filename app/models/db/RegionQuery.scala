package models.db

import esecuele.Column.{column, literal}
import esecuele.*
import models.db.OrderBy
import utils.OTLogging

case class RegionQuery(chromosome: String,
                       start: Int,
                       end: Int,
                       selectColumn: String,
                       tableName: String,
                       offset: Int,
                       size: Int,
                       sortBy: Option[OrderBy] = None
) extends Queryable
    with OTLogging {

  private val sanitisedChromosome = chromosome.replace("chr", "")

  private val array: Column = Functions.groupArray(column(selectColumn))
  private val sortedArray: Column = sortBy match {
    case Some(order) =>
      order.direction match {
        case sortDirection.ASC  => Functions.arraySort(order.lambda, array)
        case sortDirection.DESC => Functions.arrayReverseSort(Some(order.lambda), array)
      }
    case None =>
      array
  }

  private val positionalQuery = Where(
    Functions.and(
      Functions.equals(column("chromosome"), literal(sanitisedChromosome)),
      Functions.greaterOrEquals(column("start"), literal(start)),
      Functions.lessOrEquals(column("end"), literal(end))
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
      With(
        sortedArray ::
          Functions.cast(Functions.length(sortedArray), "UInt32").as(Some("count")) ::
          Functions.arraySlice(sortedArray, offset + 1, size).as(Some("rows")) ::
          Nil
      ),
      Select(column("count") :: column("rows") :: Nil),
      From(column(tableName)),
      positionalQuery,
      Format("JSONEachRow")
    )
}
