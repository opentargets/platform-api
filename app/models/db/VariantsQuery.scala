package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import play.api.Logging

case class VariantsQuery(ids: Seq[String], tableName: String, offset: Int, size: Int)
    extends Queryable
    with Logging {

  private val conditional = Where(
    Functions.in(column("variantId"), Functions.set(ids.map(literal).toSeq))
  )

  override val query: Query =
    Query(
      Select(
        Column.star :: Nil
      ),
      From(column(tableName)),
      conditional,
      OrderBy(column("variantId") :: Nil),
      Limit(offset, size),
      Format("JSONEachRow"),
      Settings(Map("output_format_json_escape_forward_slashes" -> "0"))
    )
}
