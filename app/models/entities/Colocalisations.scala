package models.entities

import models.Backend
import models.gql.Objects.colocalisationImp
import models.gql.TypeWithId
import sangria.schema.{Field, ListType, LongType, ObjectType, fields}

case class Colocalisations(
    count: Long,
    rows: IndexedSeq[Colocalisation],
    id: String = ""
) extends TypeWithId

object Colocalisations {
  def empty: Colocalisations = Colocalisations(0, IndexedSeq.empty)
  val colocalisationsImp: ObjectType[Backend, Colocalisations] = ObjectType(
    "Colocalisations",
    "Colocalisations",
    fields[Backend, Colocalisations](
      Field("count", LongType, description = None, resolve = _.value.count),
      Field("rows", ListType(colocalisationImp), description = None, resolve = _.value.rows)
    )
  )
}
