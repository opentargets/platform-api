package models.entities

import models.Backend
import models.entities.CredibleSet.credibleSetImp
import play.api.libs.json.JsValue
import sangria.schema.{ObjectType, Field, ListType, LongType, fields}
import models.gql.TypeWithId

case class CredibleSets(
    count: Long,
    rows: IndexedSeq[JsValue],
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
