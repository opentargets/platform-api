package models.entities

import models.Backend
import models.gql.Objects.clinicalTargetImp
import sangria.schema.{Field, ListType, LongType, ObjectType, fields}

case class ClinicalTargets(
    count: Long,
    rows: IndexedSeq[ClinicalTarget]
)

object ClinicalTargets {
  def empty: ClinicalTargets = ClinicalTargets(0, IndexedSeq.empty)
  val clinicalTargetsImp: ObjectType[Backend, ClinicalTargets] = ObjectType(
    "clinicalTargets",
    "",
    fields[Backend, ClinicalTargets](
      Field("count",
            LongType,
            description =
              Some("Total number of clinical targets results matching the query filters"),
            resolve = _.value.count
      ),
      Field(
        "rows",
        ListType(clinicalTargetImp),
        description = Some("List of clinical clinical targets results between drug-target pairs"),
        resolve = _.value.rows
      )
    )
  )
}
