package models.db
import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import utils.OTLogging

/** Query to get rows by a list of unique IDs (one to one mapping)
  *
  * @param ids
  *   list of IDs to query
  * @param idField
  *   name of the ID field in the table
  * @param tableName
  *   name of the table to query
  * @param offset
  *   pagination offset
  * @param size
  *   pagination size
  */
case class IdsQuery(ids: Seq[String], idField: String, tableName: String, offset: Int, size: Int)
    extends Queryable
    with OTLogging {

  private val conditional = Where(
    Functions.in(column(idField), Functions.set(ids.map(literal).toSeq))
  )

  override val query: Query =
    Query(
      Select(
        Column.star :: Nil
      ),
      From(column(tableName)),
      conditional,
      OrderBy(column(idField) :: Nil),
      Limit(offset, size),
      Format("JSONEachRow"),
      Settings(Map("output_format_json_escape_forward_slashes" -> "0"))
    )
}
