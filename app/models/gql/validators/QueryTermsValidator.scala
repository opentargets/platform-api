package models.gql.validators

import scala.concurrent.Future

case class InvalidQueryTerms(msg: String) extends Exception(msg)

object QueryTermsValidator {
  def withQueryTermsNumberValidation[T](
      queryTerms: Seq[String],
      maxNumberOfTerms: Int
  )(callback: => Future[T]): Future[T] =
    if (queryTerms.length > maxNumberOfTerms) {
      Future.failed(
        InvalidQueryTerms(
          s"Invalid query Terms request. Number of terms must not exceed ${maxNumberOfTerms}"
        )
      )
    } else {
      callback
    }
}
