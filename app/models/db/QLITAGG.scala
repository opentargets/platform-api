package models.db

import esecuele.Column._
import esecuele.{Functions => F, Query => Q, _}
import play.api.Logging

case class QLITAGG(tableName: String,
                   indexTableName: String,
                   ids: Set[String],
                   size: Int,
                   offset: Int)
    extends Queryable
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
  val T: Column = column(tableName)
  val TIdx: Column = column(indexTableName)

  private def pmidsQ(select: Seq[Column]): Q = Q(
    Select(select),
    From(TIdx),
    PreWhere(F.in(key, F.set(ids.map(literal).toSeq))),
    GroupBy(pmid.name :: Nil),
    Having(F.greaterOrEquals(F.count(pmid.name),literal(ids.size))),
    OrderBy(F.sum(relevance.name).desc :: F.any(date.name).desc :: Nil),
    Limit(offset, size)
  )

  val litQ: Q = Q(
    Select(pmid :: pmcid :: date :: sentences :: Nil),
    From(T),
    PreWhere(F.in(pmid, pmidsQ(pmid :: Nil).toColumn(None)))
  )

  val total = {
    val countQ = Q(
      Select(literal(1) :: Nil),
      From(TIdx),
      PreWhere(F.in(key, F.set(ids.map(literal).toSeq))),
      GroupBy(pmid.name :: Nil),
      Having(F.greaterOrEquals(F.count(pmid.name),literal(ids.size)))
    )

    val q = Q(
      Select(F.count(Column.star) :: Nil),
      From(countQ.toColumn(None))
    )

    logger.debug(q.toString)

    q
  }

  // select pmid, pmcid, date, year, month, day, sentences
  //from (select pmid, sum(relevance) as r
  //    from ot.literature_index
  //    prewhere keywordId in ('EFO_0000616')
  //    group by pmid having count() > 0
  //    order by sum(relevance) desc, any(date) desc
  //    limit 10 offset 0
  //)T
  //left any join (select pmid, pmcid, date, year, month, day, sentences
  //    from ot.literature
  //    prewhere pmid in (select pmid
  //        from ot.literature_index
  //            prewhere keywordId in ('EFO_0000616')
  //        group by pmid having count() > 0
  //        order by sum(relevance) desc, any(date) desc
  //        limit 10 offset 0)
  //    ) L using pmid;
  override val query = {
    val q = Q(
      Select(pmid :: pmcid :: date :: sentences :: Nil),
      From(pmidsQ(pmid :: Nil).toColumn(None) , Some("L")),
      Join(litQ.toColumn(None), Some("left"), Some("any"), global = false, Some("L"), pmid :: Nil)
    )

    logger.debug(q.toString)

    q
  }
}
