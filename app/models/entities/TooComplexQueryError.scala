package models.entities

import akka.http.scaladsl.model.DateTime
import models.entities.Violations.InputParameterCheckError
import sangria.execution.{ExceptionHandler, HandledException, MaxQueryDepthReachedError}
import sangria.marshalling.ResultMarshaller

import scala.reflect.runtime.universe.typeOf

case object TooComplexQueryError extends Exception("Query is too expensive.") {

  private def handleExceptionWithCode(message: String, code: String, marshaller: ResultMarshaller) = {
    val additionalFields: Map[String, ResultMarshaller#Node] =
      Map(
        "code" -> marshaller.scalarNode(code, typeOf[String].toString(), Set()),
        "timestamp" -> marshaller.scalarNode(DateTime.now.toString(), typeOf[String].toString(), Set())
      )
    HandledException(message, addFieldsInExtensions = true, additionalFields = additionalFields)
  }

  lazy val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case (_, error @ TooComplexQueryError)         => HandledException(error.getMessage)
    case (_, error @ MaxQueryDepthReachedError(_)) => HandledException(error.getMessage)
    case (_, error @ InputParameterCheckError(_))  => HandledException(error.getMessage)
    case (m, error: com.sksamuel.elastic4s.http.JavaClientExceptionWrapper) => {
      handleExceptionWithCode("Error connecting to Elasticsearch. Contact system administrator.","SOURCE_UNAVAILABLE_ELASTICSEARCH", m)
    }
    case (m, error: java.sql.SQLTransientConnectionException) =>
      handleExceptionWithCode("Error connecting to the Clickhouse db. Contact system administrator.","SOURCE_UNAVAILABLE_CLICKHOUSE", m)
  }
}
