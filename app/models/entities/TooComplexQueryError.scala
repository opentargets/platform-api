package models.entities

import org.apache.pekko.http.scaladsl.model.DateTime
import models.entities.Violations.InputParameterCheckError
import models.gql.validators.InvalidQueryTerms
import sangria.execution.{ExceptionHandler, HandledException, MaxQueryDepthReachedError}
import sangria.marshalling.ResultMarshaller
import utils.OTLogging

case object TooComplexQueryError
    extends Exception(
      "Query is too expensive. The response size is likely to be too large. Try requesting smaller page sizes or fewer items"
    )
    with OTLogging {

  private def handleExceptionWithCode(message: String,
                                      code: String,
                                      marshaller: ResultMarshaller
  ) = {
    val additionalFields: Map[String, ResultMarshaller#Node] =
      Map(
        "code" -> marshaller.scalarNode(code, classOf[String].toString, Set()),
        "timestamp" -> marshaller.scalarNode(DateTime.now.toString(),
                                             classOf[String].toString,
                                             Set()
        )
      )
    HandledException(message, addFieldsInExtensions = true, additionalFields = additionalFields)
  }

  lazy val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case (_, error @ TooComplexQueryError)         => HandledException(error.getMessage)
    case (_, error @ MaxQueryDepthReachedError(_)) => HandledException(error.getMessage)
    case (_, error @ InputParameterCheckError(_))  => HandledException(error.getMessage)
    case (_, error @ InvalidQueryTerms(_))         => HandledException(error.getMessage)
    case (m, error @ com.sksamuel.elastic4s.http.JavaClientExceptionWrapper(_)) =>
      HandledException(error.getMessage)
    case (m, error: java.sql.SQLTransientConnectionException) =>
      handleExceptionWithCode(
        "Error connecting to the Clickhouse db. Contact system administrator.",
        "SOURCE_UNAVAILABLE_CLICKHOUSE",
        m
      )
  }
}
