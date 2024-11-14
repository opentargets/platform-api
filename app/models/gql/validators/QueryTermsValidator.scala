package models.gql.validators

import scala.concurrent.Future
import scala.util.{Try, Failure, Success}

case class InvalidQueryTerms(msg: String) extends Exception(msg)

object QueryTermsValidator {
  def withQueryTermsNumberValidation(
      queryTerms: Seq[String],
      maxNumberOfTerms: Int
  ): Try[Seq[String]] =
    if (queryTerms.length > maxNumberOfTerms) {

      Failure(
        InvalidQueryTerms(
          s"Invalid request. Number of requested terms must not exceed ${maxNumberOfTerms}"
        )
      )

    } else {
      Success(queryTerms)
    }
}
