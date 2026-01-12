package models.entities

import play.api.libs.json.*
import utils.OTLogging

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

object Biosample extends OTLogging {

  implicit val biosampleF: OFormat[Biosample] = Json.format[Biosample]
}
