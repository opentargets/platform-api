package models.entities

import play.api.libs.json.Json

case class HealthCheck(ok: Boolean, status: String)

object HealthCheck {
  object JSONImplicits {
    implicit val healthImp = Json.format[HealthCheck]
  }
}
