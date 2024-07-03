package models.entities

import play.api.Logging
import play.api.libs.json.{OFormat, OWrites, Json}

case class GwasIndex(studyId: String,
                     biosampleFromSourceId: Option[String],
                     geneId: Option[String],
                     hasSumstats: Option[Boolean],
                     nSamples: Option[Long],
                     projectId: Option[String],
                     studyType: Option[String],
                     summarystatsLocation: Option[String],
                     traitFromSource: Option[String]
)

object GwasIndex extends Logging {
  implicit val variantIndexF: OFormat[GwasIndex] = Json.format[GwasIndex]
}
