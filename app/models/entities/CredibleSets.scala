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
    "95% credible sets for GWAS and molQTL studies. Credible sets include all variants in the credible set as well as the fine-mapping method and statistics used to estimate the credible set.",
    fields[Backend, CredibleSets](
      Field("count",
            LongType,
            description = Some("Total number of credible sets matching the query filters"),
            resolve = _.value.count
      ),
      Field(
        "rows",
        ListType(credibleSetImp),
        description = Some(
          "List of credible set entries with their associated statistics and fine-mapping information"
        ),
        resolve = _.value.rows
      )
    )
  )
}
