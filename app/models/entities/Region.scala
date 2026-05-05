package models.entities

import utils.OTLogging
import play.api.libs.json.{Json, OFormat}
import models.gql.ChromosomeEnum

case class Region(
    chromosome: ChromosomeEnum.Value,
    start: Int,
    end: Int
)

object Region extends OTLogging {
  implicit val regionF: OFormat[Region] = Json.format[Region]
}
