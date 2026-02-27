package models.db

import esecuele.Column.{column, literal}
import esecuele.Functions as F
import esecuele.{Column, Query, *}
import utils.OTLogging

case class LiteratureQuery(ids: Set[String],
                           tableName: String,
                           filterStartDate: Option[(Int, Int)],
                           filterEndDate: Option[(Int, Int)],
                           offset: Int,
                           size: Int
) extends Queryable
    with OTLogging {
  val year: Column = column("year")
  val month: Column = column("month")
  val keywordId: Column = column("keywordId")
  val pmid: Column = column("pmid")
  val pmcid: Column = column("pmcid")
  val date: Column = column("date")
  val relevance: Column = column("relevance")

  private val yearMonth: Column = F.plus(F.multiply(year, literal(100)), month)
  private val dateStartFilter =
    filterStartDate match
      case Some(value) =>
        Some(
          F.greaterOrEquals(
            yearMonth,
            literal((value._1 * 100) + value._2)
          )
        )
      case _ => None

  private val dateEndFilter =
    filterEndDate match
      case Some(value) =>
        Some(
          F.lessOrEquals(
            yearMonth,
            literal((value._1 * 100) + value._2)
          )
        )
      case _ => None

  private val withSection = With(
    Seq(
      Query(
        Select(F.min(year) :: Nil),
        From(column(tableName)),
        Where(F.in(keywordId, F.set(ids.map(literal).toSeq)))
      ).toColumn(None).as(Some("ly")),
      Query(
        Select(F.count(pmid) :: Nil),
        From(column(tableName)),
        Where(F.in(keywordId, F.set(ids.map(literal).toSeq)))
      ).toColumn(None).as(Some("c")),
      Query(
        Select(F.count(pmid) :: Nil),
        From(column(tableName)),
        Where(
          F.and(
            F.in(keywordId, F.set(ids.map(literal).toSeq)),
            F.and(
              dateStartFilter.getOrElse(literal(true)),
              dateEndFilter.getOrElse(literal(true))
            )
          )
        )
      ).toColumn(None).as(Some("fc"))
    )
  )

  private val select = Select(
    Seq(
      F.cast(column("c"), "UInt32").as(Some("count")),
      F.cast(column("ly"), "UInt32").as(Some("earliestPubYear")),
      F.cast(column("fc"), "UInt32").as(Some("filteredCount")),
      Functions
        .arraySlice(
          F.arrayReverseSort(
            lambda = Some("p -> (p.relevance, p.date)"),
            col = F.groupArray(
              F.cast(
                F.tuple(pmid :: pmcid :: date :: year :: month :: relevance :: Nil),
                "Tuple(pmid String, pmcid Nullable(String), date Date, year UInt16, month UInt8, relevance Float64)"
              )
            )
          ),
          offset + 1,
          size
        )
        .as(Some("rows"))
    )
  )
  private val from = From(column(tableName))
  private val where = Where(
    F.and(
      F.in(keywordId, F.set(ids.map(literal).toSeq)),
      F.and(
        dateStartFilter.getOrElse(literal(true)),
        dateEndFilter.getOrElse(literal(true))
      )
    )
  )
  override val query: Query =
    Query(
      withSection,
      select,
      from,
      where,
      Format("JSONEachRow")
    )

}
