package models.entities

import play.api.Logging
import play.api.libs.json._

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
    otherStudyLocusId: Option[String]
)

object Colocalisation extends Logging {
  implicit val colocalisationF: OFormat[Colocalisation] = Json.format[Colocalisation]
}
