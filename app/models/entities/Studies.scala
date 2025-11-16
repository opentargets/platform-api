package models.entities

import models.Backend
import models.gql.Objects.studyImp
import play.api.libs.json.JsValue
import sangria.schema.{ObjectType, Field, ListType, LongType, fields}

case class Studies(
    count: Long,
    rows: IndexedSeq[Study]
)

object Studies {
  def empty: Studies = Studies(0, IndexedSeq.empty)
  val studiesImp: ObjectType[Backend, Studies] = ObjectType(
    "Studies",
    "List of GWAS and molecular QTL studies with total count",
    fields[Backend, Studies](
      Field(
        "count",
        LongType,
        description = Some("Total number of studies matching the query"),
        resolve = _.value.count
      ),
      Field(
        "rows",
        ListType(studyImp),
        description = Some("List of GWAS or molecular QTL studies"),
        resolve = _.value.rows
      )
    )
  )
}
