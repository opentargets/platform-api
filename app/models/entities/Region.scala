package models.entities

import utils.OTLogging
import play.api.libs.json.*
import models.gql.ChromosomeEnum

case class Region(
    chromosome: ChromosomeEnum.Value,
    start: Int,
    end: Int
)

object Region extends OTLogging {
  val rangeMax = 5000000
  implicit val regionW: OFormat[Region] = Json.format[Region]
}
