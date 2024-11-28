package models.entities

import models.Backend
import models.entities.Pagination
import models.gql.Fetchers.{variantFetcher}
import models.gql.Objects.{logger, variantIndexImp}
import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.{Reads, JsValue, Json, OFormat, OWrites}
import play.api.libs.functional.syntax._
import sangria.schema.{
  DeferredValue,
  Field,
  FloatType,
  IntType,
  ListType,
  ObjectType,
  OptionType,
  StringType,
  fields
}
import models.gql.Arguments.{studyTypes, pageArg, pageSize, variantIds}
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

  implicit val locusImp: ObjectType[Backend, Locus] = deriveObjectType[Backend, Locus](
    ReplaceField(
      "variantId",
      Field(
        "variant",
        OptionType(variantIndexImp),
        description = None,
        resolve = r => {
          val variantId = r.value.variantId.getOrElse("")
          logger.debug(s"Finding variant index: $variantId")
          variantFetcher.deferOpt(variantId)
        }
      )
    )
  )

  implicit val lociImp: ObjectType[Backend, Loci] = deriveObjectType[Backend, Loci](
    ExcludeFields("id")
  )
  implicit val locusF: OFormat[Locus] = Json.format[Locus]
  implicit val lociR: Reads[Loci] = (
    (JsPath \ "count").read[Long] and
      (JsPath \ "locus").readNullable[Seq[Locus]] and
      (JsPath \ "studyLocusId").read[String]
  )(Loci.apply _)

}
