package models.entities

import models.Backend
import models.entities.Study.studyImp
import play.api.libs.json.JsValue
import sangria.schema.{ObjectType, Field, ListType, LongType, fields}

case class Studies(
    count: Long,
    rows: IndexedSeq[JsValue]
)

object Studies {
  def empty: Studies = Studies(0, IndexedSeq.empty)
  val studiesImp: ObjectType[Backend, Studies] = ObjectType(
    "Studies",
    "Studies",
    fields[Backend, Studies](
      Field("count", LongType, description = None, resolve = _.value.count),
      Field("rows", ListType(studyImp), description = None, resolve = _.value.rows)
    )
  )
}
