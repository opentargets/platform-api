package models.db

import esecuele.Column.*
import esecuele.{Functions as F, Query as Q, *}
import net.logstash.logback.argument.StructuredArguments.keyValue
import utils.OTLogging

case class QLITAGG(
    tableName: String,
    indexTableName: String,
    ids: Set[String],
    size: Int,
    offset: Int,
    filterStartDate: Option[(Int, Int)],
    filterEndDate: Option[(Int, Int)]
) extends Queryable
    with OTLogging {

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

  private val pmidsPrewhereFrom =
    Seq(From(TIdx), PreWhere(F.in(key, F.set(ids.map(literal).toSeq))))

  private val pmidsGroupingSections =
    Seq(GroupBy(pmid.name :: Nil), Having(F.greaterOrEquals(F.count(pmid.name), literal(ids.size))))

  private def pmidsQ(select: Seq[Column]): Q = Q(
    Select(select),
    pmidsPrewhereFrom ++ pmidsGroupingSections*
  )

  val filteredTotalQ: Q = {
    val preCountBaseQ = Q(
      Select(literal(1) :: Nil),
      From(T),
      PreWhere(F.in(pmid, pmidsQ(pmid :: Nil).toColumn(None)))
    )
    val preCountQ = (dateStartFilter, dateEndFilter) match {
      case (Some(start), None) => preCountBaseQ.copy(preCountBaseQ.sections :+ Where(start))
      case (None, Some(end))   => preCountBaseQ.copy(preCountBaseQ.sections :+ Where(end))
      case (Some(start), Some(end)) =>
        preCountBaseQ.copy(preCountBaseQ.sections :+ Where(F.and(start, end)))
      case _ => preCountBaseQ
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

    logger.debug(q.toString,
                 keyValue("query_name", "countQ"),
                 keyValue("query_type", this.getClass.getName)
    )

    q
  }

  val minDate: Q = {
    def pmidsOrderedQ(select: Seq[Column]): Q = Q(
      Select(select),
      (pmidsPrewhereFrom ++ pmidsGroupingSections :+ OrderBy(
        F.sum(relevance.name).desc :: F.any(date.name).desc :: Nil
      ))*
    )
    val q = Q(
      Select(F.min(year) :: Nil),
      From(T),
      PreWhere(
        F.and(F.in(pmid, pmidsOrderedQ(pmid :: Nil).toColumn(None)), F.greater(year, literal(0)))
      )
    )

    logger.debug(q.toString,
                 keyValue("query_name", "minDate"),
                 keyValue("query_type", this.getClass.getName)
    )

    q
  }

  private def dateStartFilter =
    filterStartDate match
      case Some(value) =>
        Some(
          F.greaterOrEquals(
            F.plus(F.multiply(year, literal(100)), month),
            literal((value._1 * 100) + value._2)
          )
        )
      case _ => None

  private def dateEndFilter =
    filterEndDate match
      case Some(value) =>
        Some(
          F.lessOrEquals(
            F.plus(F.multiply(year, literal(100)), month),
            literal((value._1 * 100) + value._2)
          )
        )
      case _ => None

  override val query: Q = {
    val ctePmids = Column("cte_pmids")

    val litQuery = Q(
      Select(pmid :: pmcid :: date :: year :: month :: Nil),
      From(T),
      PreWhere(F.in(pmid, ctePmids))
    )

    val withBaseQ = Q(Select(pmid :: Nil), pmidsPrewhereFrom*)

    val groupPaginateSections = pmidsGroupingSections ++ Seq(
      OrderBy(F.sum(relevance.name).desc :: F.any(date.name).desc :: Nil),
      Limit(offset, size)
    )

    val withQuery = (dateStartFilter, dateEndFilter) match {
      case (Some(startFilter), None) =>
        withBaseQ.copy((withBaseQ.sections :+ Where(startFilter)) ++ groupPaginateSections)
      case (None, Some(endFilter)) =>
        withBaseQ.copy((withBaseQ.sections :+ Where(endFilter)) ++ groupPaginateSections)
      case (Some(startFilter), Some(endFilter)) =>
        withBaseQ.copy(
          (withBaseQ.sections :+ Where(F.and(startFilter, endFilter))) ++ groupPaginateSections
        )
      case _ => withBaseQ.copy(withBaseQ.sections ++ groupPaginateSections)
    }

    val query = Q(
      With(Seq(withQuery.toColumn(None)), Some(ctePmids)),
      Select(pmid :: pmcid :: date :: year :: month :: Nil),
      From(ctePmids),
      Join(litQuery.toColumn(None), None, Some("INNER"), false, Some("r"), pmid :: Nil)
    )

    logger.debug(query.toString,
                 keyValue("query_name", "query"),
                 keyValue("query_type", this.getClass.getName)
    )

    query
  }
}
