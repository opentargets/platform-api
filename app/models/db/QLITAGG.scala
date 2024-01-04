package models.db

import akka.http.scaladsl.model.DateTime
import esecuele.Column._
import esecuele.{Functions => F, Query => Q, _}
import play.api.Logging

case class QLITAGG(
    tableName: String,
    indexTableName: String,
    ids: Set[String],
    size: Int,
    offset: Int,
    filterDate: Option[(Int, Int, Int, Int)]
) extends Queryable
    with Logging {

  require(ids.nonEmpty)

  val pmid: Column = column("pmid")
  val pmcid: Column = column("pmcid")
  val key: Column = column("keywordId")
  val relevance: Column = column("relevance")
  val date: Column = column("date")
  val year: Column = column("year")
  val month: Column = column("month")
  val day: Column = column("day")
  val T: Column = column(tableName)
  val TIdx: Column = column(indexTableName)

  private def pmidsQ(select: Seq[Column]): Q = Q(
    Select(select),
    From(TIdx),
    PreWhere(F.in(key, F.set(ids.map(literal).toSeq))),
    GroupBy(pmid.name :: Nil),
    Having(F.greaterOrEquals(F.count(pmid.name), literal(ids.size))),
    OrderBy(F.sum(relevance.name).desc :: F.any(date.name).desc :: Nil)
  )

  private def pmidsQNord(select: Seq[Column]): Q = Q(
    Select(select),
    From(TIdx),
    PreWhere(F.in(key, F.set(ids.map(literal).toSeq))),
    GroupBy(pmid.name :: Nil),
    Having(F.greaterOrEquals(F.count(pmid.name), literal(ids.size)))
  )

  val filteredTotalQ: Q = {
    val preCountQ = filterDate match {
      case Some(value) =>
        Q(
          Select(literal(1) :: Nil),
          From(T),
          PreWhere(F.in(pmid, pmidsQNord(pmid :: Nil).toColumn(None))),
          Where(
            F.and(
              F.greaterOrEquals(
                F.plus(F.multiply(year, literal(100)), month),
                literal((value._1 * 100) + value._2)
              ),
              F.lessOrEquals(
                F.plus(F.multiply(year, literal(100)), month),
                literal((filterDate.get._3 * 100) + filterDate.get._4)
              )
            )
          )
        )
      case _ =>
        Q(
          Select(literal(1) :: Nil),
          From(T),
          PreWhere(F.in(pmid, pmidsQNord(pmid :: Nil).toColumn(None)))
        )
    }

    Q(
      Select(F.count(Column.star) :: Nil),
      From(preCountQ.toColumn(None))
    )
  }

  val total: Q = {
    val countQ = Q(
      Select(literal(1) :: Nil),
      From(TIdx),
      PreWhere(F.in(key, F.set(ids.map(literal).toSeq))),
      GroupBy(pmid.name :: Nil),
      Having(F.greaterOrEquals(F.count(pmid.name), literal(ids.size)))
    )

    val q = Q(
      Select(F.count(Column.star) :: Nil),
      From(countQ.toColumn(None))
    )

    logger.debug(q.toString)

    q
  }

  val minDate: Q = {
    val q = Q(
      Select(F.min(year) :: Nil),
      From(T),
      PreWhere(F.in(pmid, pmidsQ(pmid :: Nil).toColumn(None)))
    )

    logger.debug(q.toString)

    q
  }

  override val query: Q = {

    val q = filterDate match {
      case Some(value) =>
        Q(
          Select(pmid :: pmcid :: date :: year :: month :: Nil),
          From(T),
          PreWhere(F.in(pmid, pmidsQ(pmid :: Nil).toColumn(None))),
          Where(
            F.and(
              F.greaterOrEquals(
                F.plus(F.multiply(year, literal(100)), month),
                literal((value._1 * 100) + value._2)
              ),
              F.lessOrEquals(
                F.plus(F.multiply(year, literal(100)), month),
                literal((filterDate.get._3 * 100) + filterDate.get._4)
              )
            )
          ),
          Limit(offset, size)
        )
      case _ =>
        Q(
          Select(pmid :: pmcid :: date :: year :: month :: Nil),
          From(T),
          PreWhere(F.in(pmid, pmidsQ(pmid :: Nil).toColumn(None))),
          Limit(offset, size)
        )
    }

    logger.debug(q.toString)

    q
  }
}
