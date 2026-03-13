package models.entities

import models.Backend
import models.gql.Objects.{clinicalIndicationFromDiseaseImp, clinicalIndicationFromDrugImp}
import sangria.schema.{Field, ListType, LongType, ObjectType, fields}

case class ClinicalIndications(
    count: Long,
    rows: IndexedSeq[ClinicalIndication]
)

object ClinicalIndications {
  def empty: ClinicalIndications = ClinicalIndications(0, IndexedSeq.empty)
  val clinicalIndicationsFromDrugImp: ObjectType[Backend, ClinicalIndications] = ObjectType(
    "clinicalIndicationsFromDrugImp",
    "",
    fields[Backend, ClinicalIndications](
      Field("count",
            LongType,
            description = Some("Total number of indications results matching the query filters"),
            resolve = _.value.count
      ),
      Field(
        "rows",
        ListType(clinicalIndicationFromDrugImp),
        description = Some("List of clinical indications results between drug-disease pairs"),
        resolve = _.value.rows
      )
    )
  )
  val clinicalIndicationsFromDiseaseImp: ObjectType[Backend, ClinicalIndications] = ObjectType(
    "clinicalIndicationsFromDiseaseImp",
    "",
    fields[Backend, ClinicalIndications](
      Field("count",
            LongType,
            description = Some("Total number of indications results matching the query filters"),
            resolve = _.value.count
      ),
      Field(
        "rows",
        ListType(clinicalIndicationFromDiseaseImp),
        description = Some("List of clinical indications results between drug-disease pairs"),
        resolve = _.value.rows
      )
    )
  )
}
