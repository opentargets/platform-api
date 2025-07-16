package models.entities

import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.*

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

object Biosample {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val biosampleF: OFormat[Biosample] = Json.format[Biosample]
}
