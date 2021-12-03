package models.entities

import models.entities.Violations.InputParameterCheckError
import sangria.execution.{ExceptionHandler, HandledException, MaxQueryDepthReachedError}

case object TooComplexQueryError extends Exception("Query is too expensive.") {
  lazy val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case (_, error@TooComplexQueryError) => HandledException(error.getMessage)
    case (_, error@MaxQueryDepthReachedError(_)) => HandledException(error.getMessage)
    case (_, error@InputParameterCheckError(_)) => HandledException(error.getMessage)
  }
}
