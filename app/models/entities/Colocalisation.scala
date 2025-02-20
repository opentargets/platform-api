package models.entities

import play.api.libs.json.{Reads, JsValue, Json, OFormat, OWrites}

case class Colocalisation(
    leftStudyLocusId: String,
    rightStudyLocusId: String,
    rightStudyType: String,
    chromosome: String,
    colocalisationMethod: String,
    numberColocalisingVariants: Long,
    h3: Option[Double],
    h4: Option[Double],
    clpp: Option[Double],
    betaRatioSignAverage: Option[Double],
    otherStudyLocusId: Option[String]
)

object Colocalisation {
  implicit val colocalisationF: OFormat[Colocalisation] = Json.format[Colocalisation]
}
