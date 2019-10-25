package models.entities

import play.api.libs.json.Json

case class APIErrorMessage(code: Int, message: String)

object APIErrorMessage {
  object JSONImplicits {
    implicit val apiErrorMessageImp = Json.format[APIErrorMessage]
  }
}
