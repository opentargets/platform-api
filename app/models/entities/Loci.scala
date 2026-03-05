package models.entities

import play.api.libs.json._
import models.gql.TypeWithId
import slick.jdbc.GetResult
import utils.OTLogging
import utils.db.DbJsonParser.fromPositionedResult

case class Locus(
    variantId: String,
    posteriorProbability: Double,
    pValueMantissa: Option[Double],
    pValueExponent: Option[Int],
    logBF: Option[Double],
    beta: Option[Double],
    standardError: Option[Double],
    is95CredibleSet: Boolean,
    is99CredibleSet: Boolean,
    r2Overall: Option[Double]
)

case class Loci(
    count: Long,
    rows: Option[Seq[Locus]],
    id: String
) extends TypeWithId

object Loci extends OTLogging {
  def empty(): Loci = Loci(0, None, "")
  implicit val getResultLoci: GetResult[Loci] =
    GetResult(fromPositionedResult[Loci])
  implicit val locusF: OFormat[Locus] = Json.format[Locus]
  implicit val lociF: OFormat[Loci] = Json.format[Loci]
}
