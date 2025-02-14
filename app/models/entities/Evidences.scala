package models.entities

import models.Backend
import sangria.schema.{Field, ListType, LongType, ObjectType, OptionType, StringType, fields}
import models.gql.Objects.evidenceImp

case class Evidences(count: Long, cursor: Option[String], rows: IndexedSeq[Evidence])

object Evidences {
  def empty(withTotal: Long = 0): Evidences = Evidences(withTotal, None, IndexedSeq.empty)

  val evidencesImp: ObjectType[Backend, Evidences] = ObjectType(
    "Evidences",
    "Evidence for a Target-Disease pair",
    fields[Backend, Evidences](
      Field("count", LongType, description = None, resolve = _.value.count),
      Field("cursor", OptionType(StringType), description = None, resolve = _.value.cursor),
      Field("rows", ListType(evidenceImp), description = None, resolve = _.value.rows)
    )
  )
}
