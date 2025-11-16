package models.entities

import models.Backend
import sangria.schema.{Field, ListType, LongType, ObjectType, OptionType, StringType, fields}
import models.gql.Objects.evidenceImp

case class Evidences(count: Long, cursor: Option[String], rows: IndexedSeq[Evidence])

object Evidences {
  def empty(withTotal: Long = 0): Evidences = Evidences(withTotal, None, IndexedSeq.empty)

  val evidencesImp: ObjectType[Backend, Evidences] = ObjectType(
    "Evidences",
    "Target–disease evidence items with total count and pagination cursor",
    fields[Backend, Evidences](
      Field("count", LongType, description = Some("Total number of evidence items available for the query"), resolve = _.value.count),
      Field("cursor", OptionType(StringType), description = Some("Opaque pagination cursor to request the next page of results"), resolve = _.value.cursor),
      Field("rows", ListType(evidenceImp), description = Some("List of evidence items supporting the target–disease association"), resolve = _.value.rows)
    )
  )
}
