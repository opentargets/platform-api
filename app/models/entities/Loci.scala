package models.entities

import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.{Reads, JsValue, Json, OFormat, OWrites}
import play.api.libs.functional.syntax._
import models.gql.TypeWithId

case class Locus(
    variantId: Option[String],
    posteriorProbability: Option[Double],
    pValueMantissa: Option[Double],
    pValueExponent: Option[Int],
    logBF: Option[Double],
    beta: Option[Double],
    standardError: Option[Double],
    is95CredibleSet: Option[Boolean],
    is99CredibleSet: Option[Boolean],
    r2Overall: Option[Double]
)

case class Loci(
    count: Long,
    rows: Option[Seq[Locus]],
    id: String
) extends TypeWithId

object Loci extends Logging {
  import sangria.macros.derive._
  def empty(): Loci = Loci(0, None, "")

  implicit val locusF: OFormat[Locus] = Json.format[Locus]
  implicit val lociR: Reads[Loci] = (
    (JsPath \ "count").read[Long] and
      (JsPath \ "locus").readNullable[Seq[Locus]] and
      (JsPath \ "studyLocusId").read[String]
  )(Loci.apply)

}
