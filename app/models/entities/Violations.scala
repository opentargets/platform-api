package models.entities

import sangria.execution.{UserFacingError, WithViolations}
import sangria.validation.{BaseViolation, Violation}

object Violations {

  val dateFilterErrorMsg = "Invalid arguments, %s is required if you set %s"

  case class DateFilterError(yearField: String, monthField: String)
      extends BaseViolation(dateFilterErrorMsg format (yearField, monthField))

  val invalidArgValueErrorMsg =
    "The value for the argument '%s' is invalid. The value should be %s. %s"

  /** This error can be used when an argument has been assigned an invalid value. The message will
    * be in the format "The value for the argument 'argument' is invalid. The value should be
    * 'valueMsg'. 'additionalMsg'"
    * @param argument:
    *   Name of the invalid argument.
    * @param valueMsg:
    *   Message specifying the correct value for the argument.
    * @param additionalMsg:
    *   Optional additional message relevant to the argument.
    */
  case class InvalidArgValueError(argument: String,
                                  valueMsg: String,
                                  additionalMsg: Option[String] = None
  ) extends BaseViolation(
        invalidArgValueErrorMsg format (argument, valueMsg, additionalMsg.getOrElse(""))
      )

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
