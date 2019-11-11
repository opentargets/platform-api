package models

import models.entities.Violations.InputParameterCheckError
import play.api.libs.json.Json
import sangria.execution.{ExceptionHandler, HandledException, MaxQueryDepthReachedError}

object Entities {
  case class TargetsBody(ids: Seq[String])

  case object TooComplexQueryError extends Exception("Query is too expensive.")

  lazy val exceptionHandler = ExceptionHandler {
    case (_, error @ TooComplexQueryError) => HandledException(error.getMessage)
    case (_, error @ MaxQueryDepthReachedError(_)) => HandledException(error.getMessage)
    case (_, error @ InputParameterCheckError(_)) => HandledException(error.getMessage)
  }

  object JSONImplicits {
    implicit val targetsBodyImp = Json.format[Entities.TargetsBody]
  }
}
