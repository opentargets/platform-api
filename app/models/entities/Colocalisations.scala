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
    "GWAS-GWAS and GWAS-molQTL credible set colocalisation results. Dataset includes colocalising pairs as well as the method and statistics used to estimate the colocalisation.",
    fields[Backend, Colocalisations](
      Field("count",
            LongType,
            description = Some("Total number of colocalisation results matching the query filters"),
            resolve = _.value.count
      ),
      Field("rows",
            ListType(colocalisationImp),
            description = Some("List of colocalisation results between study-loci pairs"),
            resolve = _.value.rows
      )
    )
  )
}
