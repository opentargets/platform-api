package models.entities

import models.Backend
import models.gql.Objects.colocalisationImp
import models.gql.TypeWithId
import slick.jdbc.GetResult
import play.api.libs.json.{Reads, JsValue, Json, OFormat, OWrites}
import sangria.schema.{Field, ListType, LongType, ObjectType, fields}

case class Colocalisation(
    studyLocusId: String,
    otherStudyLocusId: String,
    otherStudyType: String,
    chromosome: String,
    colocalisationMethod: String,
    numberColocalisingVariants: Long,
    h3: Option[Double],
    h4: Option[Double],
    clpp: Option[Double],
    betaRatioSignAverage: Option[Double],
    metaTotal: Int
)
case class Colocalisations(
    count: Long,
    rows: IndexedSeq[Colocalisation],
    id: String = ""
) extends TypeWithId

object Colocalisations {
  implicit val getRowFromDB: GetResult[Colocalisation] =
    GetResult(r => Json.parse(r.<<[String]).as[Colocalisation])
  implicit val colocalisationF: OFormat[Colocalisation] = Json.format[Colocalisation]
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
