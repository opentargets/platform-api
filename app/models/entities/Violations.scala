package models.entities

import sangria.execution.{UserFacingError, WithViolations}
import sangria.validation.{BaseViolation, Violation}

object Violations {
  val paginationSizeErrorMsg: String =
    "There was a pagination error. You used size %d but the size must be between 0 and %d"

  case class PaginationSizeError(currentSize: Int, sizeMax: Int = Pagination.sizeMax)
      extends BaseViolation(paginationSizeErrorMsg format (currentSize, sizeMax))

  val paginationIndexErrorMsg: String =
    "There was a pagination error. You used index %d but the index must be 0 or greater"

  case class PaginationIndexError(currentIndex: Int)
      extends BaseViolation(paginationIndexErrorMsg format (currentIndex))

  case class InputParameterCheckError(violations: Vector[Violation])
      extends Exception(
        s"Error during input parameter check. " +
          s"Violations:\n\n${violations map (_.errorMessage) mkString "\n\n"}"
      )
      with WithViolations
      with UserFacingError
}
