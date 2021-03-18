package models.entities

import models.Backend
import models.entities.Publication._
import play.api.libs.json.{JsValue, Json}
import sangria.schema.{Field, ListType, LongType, ObjectType, OptionType, StringType, fields}

case class Publications(count: Long, cursor: Option[String], rows: IndexedSeq[JsValue])

object Publications {
  def empty(withTotal: Long = 0) = Publications(withTotal, None, IndexedSeq.empty)

  val publicationsImp = ObjectType(
    "Publications",
    "Publication list",
    fields[Backend, Publications](
      Field("count", LongType, description = None, resolve = _.value.count),
      Field("cursor",
        OptionType(StringType),
        description = None,
        resolve = _.value.cursor),
      Field("rows", ListType(publicationImp), description = None, resolve = _.value.rows)
    )
  )
}
