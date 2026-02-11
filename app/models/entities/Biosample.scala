package models.entities

import play.api.Logging
import play.api.libs.json._
import slick.jdbc.GetResult

case class Biosample(
    biosampleId: String,
    biosampleName: String,
    description: Option[String],
    xrefs: Option[Seq[String]],
    synonyms: Option[Seq[String]],
    parents: Option[Seq[String]],
    ancestors: Option[Seq[String]],
    children: Option[Seq[String]],
    descendants: Option[Seq[String]]
)

object Biosample extends Logging {
  implicit val getResultBiosample: GetResult[Biosample] =
    GetResult(r => Json.parse(r.<<[String]).as[Biosample])
  implicit val biosampleF: OFormat[Biosample] = Json.format[Biosample]
}
