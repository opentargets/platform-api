package models.entities

import models.Backend
import models.entities.Evidence.evidenceImp
import play.api.libs.json.JsValue
import sangria.schema.{Field, ListType, LongType, ObjectType, OptionType, StringType, fields}

case class Evidences(count: Long, cursor: Option[String], rows: IndexedSeq[JsValue])

object Evidences {
  def empty(withTotal: Long = 0): Evidences = Evidences(withTotal, None, IndexedSeq.empty)

  val evidencesImp: ObjectType[Backend, Evidences] = ObjectType(
    "Evidences",
    "Evidence for a Target-Disease pair",
    fields[Backend, Evidences](
      Field("count", LongType, description = None, resolve = _.value.count),
      Field("cursor",
        OptionType(StringType),
        description = None,
        resolve = _.value.cursor),
      Field("rows", ListType(evidenceImp), description = None, resolve = _.value.rows)
    )
  )
}
