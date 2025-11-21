package models.entities

import models.Backend
import models.entities.Publication._
import play.api.libs.json.JsValue
import sangria.schema.{
  Field,
  IntType,
  ListType,
  LongType,
  ObjectType,
  OptionType,
  StringType,
  fields
}

case class Publications(count: Long,
                        lowYear: Int,
                        cursor: Option[String],
                        rows: IndexedSeq[JsValue],
                        filteredCount: Long = 0
)

object Publications {
  def empty(withTotal: Long = 0, withLowYear: Int = 0): Publications =
    Publications(withTotal, withLowYear, None, IndexedSeq.empty)

  val publicationsImp: ObjectType[Backend, Publications] = ObjectType(
    "Publications",
    "List of referenced publications with total counts, earliest year and pagination cursor",
    fields[Backend, Publications](
      Field("count", LongType, description = Some("Total number of publications matching the query"), resolve = _.value.count),
      Field("filteredCount", LongType, description = Some("Number of publications after applying filters"), resolve = _.value.filteredCount),
      Field("earliestPubYear",
            IntType,
            description = Some("Earliest publication year."),
            resolve = _.value.lowYear
      ),
      Field("cursor", OptionType(StringType), description = Some("Opaque pagination cursor to request the next page of results"), resolve = _.value.cursor),
      Field("rows", ListType(publicationImp), description = Some("List of publications"), resolve = _.value.rows)
    )
  )
}
