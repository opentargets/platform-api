package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import play.api.Logging

case class IntervalsQuery(chromosome: String, start: Int, end: Int, tableName: String)
    extends Queryable
    with Logging {
  val query = {
    val q: Query = IntervalsQuery.getQuery(chromosome, start, end, tableName)
    logger.debug(q.toString)
    q
  }
}

object IntervalsQuery extends Logging {

  private def getQuery(chromosome: String, start: Int, end: Int, tableName: String) = Query(
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
    Where(
      Functions.and(
        Functions.equals(column("chromosome"), literal(chromosome)),
        Functions.greaterOrEquals(column("start"), literal(start)),
        Functions.lessOrEquals(column("end"), literal(end))
      )
    )
  )
}
