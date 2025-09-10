package models.entities

import play.api.libs.json.*
import play.api.libs.json.{JsValue, Json, OFormat, OWrites, Reads}
import play.api.libs.functional.syntax.*
import models.gql.TypeWithId
import org.slf4j.{Logger, LoggerFactory}

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

object Loci {
  import sangria.macros.derive._

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def empty(): Loci = Loci(0, None, "")

  implicit val locusF: OFormat[Locus] = Json.format[Locus]
  implicit val lociR: Reads[Loci] = (
    (JsPath \ "count").read[Long] and
      (JsPath \ "locus").readNullable[Seq[Locus]] and
      (JsPath \ "studyLocusId").read[String]
  )(Loci.apply)

}
