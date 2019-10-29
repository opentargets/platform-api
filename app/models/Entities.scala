package models

import play.api.libs.json.Json

object Entities {
  case class TargetsBody(ids: Seq[String])

  object JSONImplicits {
    implicit val targetsBodyImp = Json.format[Entities.TargetsBody]
  }
}
