package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Disease(id: String,
                   name: String,
                   therapeuticAreas: Seq[String],
                   description: Option[String],
                   synonyms: Seq[String]
                  )

object Disease {
  val logger = Logger(this.getClass)

  object JSONImplicits {
    implicit val diseaseImpW = Json.format[models.entities.Disease]
  }
}
