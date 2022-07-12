package models.db

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
  val sentences: Column = column("sentences")
  val T: Column = column(tableName) // "literature" in DB under default conditions
  val TIdx: Column = column(indexTableName) // "literature_index" in DB under default conditions

  private def createDatePreWhere(dates: Option[(Int, Int, Int, Int)]): PreWhere = {
    dates match {
      case Some(date) =>
        PreWhere(
          F.and(
            F.in(key, F.set(ids.map(literal).toSeq)),
            createDateFilter(date)
          ))
      case None =>
        PreWhere(F.in(key, F.set(ids.map(literal).toSeq)))
    }
  }

  // Find pmids related to Ids from literature_index
  private def pmidsQ(select: Seq[Column], dates: Option[(Int, Int, Int, Int)] = None): Q = {
    Q(
      Select(select),
      From(TIdx),
      createDatePreWhere(dates),
      GroupBy(pmid.name :: Nil),
      Having(F.greaterOrEquals(F.count(pmid.name), literal(ids.size))),
      OrderBy(F.sum(relevance.name).desc :: F.any(date.name).desc :: Nil),
      Limit(offset, size)
    )
  }

  private def createDateFilter(value: (Int, Int, Int, Int)): Column = {
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
  }


  /** Return the total number of publications with a selected date range, and the earliest
   * publication year as tuple (Long, Int). If no date range is selected, a default of 1900 is
   * used.
   */
  val total: Q = {
    val q = Q(
      Select(F.countDistinct(pmid) :: F.min(year) :: Nil),
      From(TIdx),
      createDatePreWhere(filterDate)
    )

    logger.debug(q.toString)

    q
  }

  override val query: Q = {

    val q = Q(
      Select(pmid :: pmcid :: date :: year :: month :: sentences :: Nil),
      From(T),
      PreWhere(F.in(pmid, pmidsQ(pmid :: Nil, filterDate).toColumn()))
    )

    logger.debug(q.toString)

    q
  }
}
