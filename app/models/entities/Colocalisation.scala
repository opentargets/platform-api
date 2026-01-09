package models.entities

import play.api.libs.json.{Reads, JsValue, Json, OFormat, OWrites}
import slick.jdbc.GetResult

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
    meta_total: Int
)

object Colocalisation {
  implicit val getRowFromDB: GetResult[Colocalisation] =
    GetResult(r => Json.parse(r.<<[String]).as[Colocalisation])
  implicit val colocalisationF: OFormat[Colocalisation] = Json.format[Colocalisation]
}
