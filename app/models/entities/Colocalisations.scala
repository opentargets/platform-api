package models.entities

import models.Backend
import models.gql.TypeWithId
import slick.jdbc.GetResult
import play.api.libs.json.{Reads, JsValue, Json, OFormat, OWrites}

case class Colocalisation(
    studyLocusId: String,
    otherStudyLocusId: String,
    rightStudyType: String,
    chromosome: String,
    colocalisationMethod: String,
    numberColocalisingVariants: Long,
    h3: Option[Double],
    h4: Option[Double],
    clpp: Option[Double],
    betaRatioSignAverage: Option[Double]
)
case class Colocalisations(
    count: Long,
    rows: IndexedSeq[Colocalisation],
    id: String = ""
) extends TypeWithId

object Colocalisations {
  implicit val getRowFromDB: GetResult[Colocalisations] =
    GetResult(r => Json.parse(r.<<[String]).as[Colocalisations])

  implicit val colocalisationsF: OFormat[Colocalisations] = Json.format[Colocalisations]
  implicit val colocalisationF: OFormat[Colocalisation] = Json.format[Colocalisation]
  def empty: Colocalisations = Colocalisations(0, IndexedSeq.empty)
}
