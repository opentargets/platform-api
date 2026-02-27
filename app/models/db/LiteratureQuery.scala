package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import utils.OTLogging

case class LiteratureQuery(ids: Set[String],
                           tableName: String,
                           filterStartDate: Option[(Int, Int)],
                           filterEndDate: Option[(Int, Int)],
                           size: Int,
                           offset: Int
) extends Queryable
    with OTLogging {
  val year: Column = column("year")
  val month: Column = column("month")
  val keywordId: Column = column("keywordId")
  val pmid: Column = column("pmid")
  val pmcid: Column = column("pmcid")
  val date: Column = column("date")
  val relevance: Column = column("relevance")

// WITH
//     (
//         SELECT min(year)
//         FROM platform2512.literature_index
//         WHERE keywordId = 'ENSG00000171862'
//     ) AS minYear,
//     (
//         SELECT count(pmid)
//         FROM platform2512.literature_index
//         WHERE keywordId = 'ENSG00000171862'
//     ) AS total,
//     (
//         SELECT count(pmid)
//         FROM platform2512.literature_index
//         WHERE (keywordId = 'ENSG00000171862') AND (year <= 2000)
//     ) AS filteredCount
// SELECT
//     total,
//     minYear,
//     filteredCount,
//     arraySlice(reverse(arraySort(p -> (p.relevance, p.date), groupArray(CAST((pmid, pmcid, date, year, month, relevance), 'Tuple(pmid String, pmcid Nullable(String), date Date, year UInt16, month UInt8, relevance Float64)')))), 1, 20) AS rows
// FROM platform2512.literature_index
// WHERE (keywordId = 'ENSG00000171862') AND (year <= 2000)

  private val withSection = With(
    Seq(
      Query(
        Select(Functions.min(year) :: Nil),
        From(column(tableName)),
        Where(Functions.in(keywordId, Functions.set(ids.map(literal).toSeq)))
      ).toColumn(Some("minYear")),
      Query(
        Select(Functions.count(pmid) :: Nil),
        From(column(tableName)),
        Where(Functions.in(keywordId, Functions.set(ids.map(literal).toSeq)))
      ).toColumn(Some("total")),
      Query(
        Select(Functions.count(pmid) :: Nil),
        From(column(tableName)),
        Where(
          Functions.and(
            Functions.in(keywordId, Functions.set(ids.map(literal).toSeq)),
            filterEndDate
              .map(end => Functions.lessOrEquals(year, literal(end)))
              .getOrElse(literal(true))
          )
        )
      ).toColumn(Some("filteredCount"))
    )
  )

  private val select = Select(
    Seq(
      column("total"),
      column("minYear"),
      column("filteredCount"),
      Functions
        .arraySlice(
          Functions.arrayReverseSort(
            lambda = Some("p -> (p.relevance, p.date)"),
            col = Functions.groupArray(
              Functions.cast(
                Functions.tuple(pmid :: pmcid :: date :: year :: month :: relevance :: Nil),
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
    Functions.and(
      Functions.in(keywordId, Functions.set(ids.map(literal).toSeq)),
      filterEndDate.map(end => Functions.lessOrEquals(year, literal(end))).getOrElse(literal(true)),
      filterStartDate
        .map(start => Functions.greaterOrEquals(year, literal(start)))
        .getOrElse(literal(true))
    )
  )
  override val query: Query =
    Query(
      withSection,
      select,
      from,
      where
    )

}
