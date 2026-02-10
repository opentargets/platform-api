package models.entities

import models.Backend
import models.gql.Objects.clinicalIndicationImp
import sangria.schema.{Field, ListType, LongType, ObjectType, fields}

case class ClinicalIndications(
    count: Long,
    rows: IndexedSeq[ClinicalIndication]
)

object ClinicalIndications {
  def empty: ClinicalIndications = ClinicalIndications(0, IndexedSeq.empty)
  val clinicalIndicationsImp: ObjectType[Backend, ClinicalIndications] = ObjectType(
    "indications",
    "",
    fields[Backend, ClinicalIndications](
      Field("count",
            LongType,
            description = Some("Total number of indications results matching the query filters"),
            resolve = _.value.count
      ),
      Field("rows",
            ListType(clinicalIndicationImp),
            description = Some("List of colocalisation results between study-loci pairs"),
            resolve = _.value.rows
      )
    )
  )
}
