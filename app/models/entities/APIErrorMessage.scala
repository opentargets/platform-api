package models.entities

import play.api.libs.json.Json

/** Rest API error wrapper to put the message and the error id */
case class APIErrorMessage(code: Int, message: String)

object APIErrorMessage {
  object JSONImplicits {
    implicit val apiErrorMessageImp = Json.format[APIErrorMessage]
  }
}
