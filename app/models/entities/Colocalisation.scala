package models.entities

import play.api.libs.json.{Reads, JsValue, Json, OFormat, OWrites}
import play.api.libs.functional.syntax._
import sangria.schema.{ObjectType, Field, ListType, LongType, fields}
import models.Backend
import models.gql.TypeWithId

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

case class Colocalisations(
    count: Long,
    rows: IndexedSeq[Colocalisation],
    id: String = ""
) extends TypeWithId

object Colocalisations {
  def empty: Colocalisations = Colocalisations(0, IndexedSeq.empty)
}
