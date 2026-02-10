package models.db

import esecuele.Column.{column, literal}
import esecuele.{Column, Format, From, Functions, Limit, OrderBy, PreWhere, Query, Select, Settings}
import utils.OTLogging

case class ClinicalIndicationQuery(id: String,
                                   tableName: String,
                                   offset: Int,
                                   size: Int,
                                   columnName: String
) extends Queryable
    with OTLogging {

  override val query: Query =
    Query(
      Select(
        Column.star :: Nil
      ),
      From(column(tableName)),
      PreWhere(
        Functions.in(column(columnName), literal(id))
      ),
      OrderBy(column("id") :: Nil),
      Limit(offset, size),
      Format("JSONEachRow"),
      Settings(Map("output_format_json_escape_forward_slashes" -> "0"))
    )
}
