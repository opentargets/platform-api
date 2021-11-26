package models.entities

import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class HealthCheck(ok: Boolean, status: String)

object HealthCheck {
  implicit val healthImp: OFormat[HealthCheck] = Json.format[HealthCheck]
}
