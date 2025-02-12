package models.entities

import models.Backend
import play.api.libs.json.JsValue
import sangria.schema.{ObjectType, Field, ListType, LongType, fields}
import models.gql.TypeWithId
import models.gql.Objects.credibleSetImp

case class CredibleSets(
    count: Long,
    rows: IndexedSeq[CredibleSet],
    id: String = ""
) extends TypeWithId

object CredibleSets {
  def empty: CredibleSets = CredibleSets(0, IndexedSeq.empty)
  val credibleSetsImp: ObjectType[Backend, CredibleSets] = ObjectType(
    "CredibleSets",
    "Credible Sets",
    fields[Backend, CredibleSets](
      Field("count", LongType, description = None, resolve = _.value.count),
      Field("rows", ListType(credibleSetImp), description = None, resolve = _.value.rows)
    )
  )
}
