package models.entities

import sangria.execution.{UserFacingError, WithViolations}
import sangria.validation.{BaseViolation, Violation}

object Violations {
  val paginationErrorMsg: String =
    "There was a pagination error. You used this size %d but the max value is %d"

  case class PaginationError(currentSize: Int, sizeMax: Int = Pagination.sizeMax)
      extends BaseViolation(paginationErrorMsg format (currentSize, sizeMax))

  case class InputParameterCheckError(violations: Vector[Violation])
      extends Exception(
        s"Error during input parameter check. " +
          s"Violations:\n\n${violations map (_.errorMessage) mkString "\n\n"}")
      with WithViolations
      with UserFacingError
}
