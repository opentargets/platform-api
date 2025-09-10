package controllers.api.v4.graphql

import QueryMetadataHeaders.{GQL_COMPLEXITY_HEADER, GQL_OP_HEADER, GQL_VAR_HEADER}
import models.Helpers.loadConfigurationObject
import models.entities.Configuration.{APIVersion, DataVersion, OTSettings}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.mvc.{ActionBuilderImpl, BodyParsers, Request, Result}

import java.sql.Timestamp
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.*

case class GqlRequestMetadata(
    isOT: Boolean,
    date: Timestamp,
    duration: Long,
    operation: String,
    variables: String,
    complexity: String,
    query: String,
    api: APIVersion,
    data: DataVersion
) {
  def jsonWritter: OWrites[GqlRequestMetadata] = Json.writes[GqlRequestMetadata]
  override def toString: String = jsonWritter.writes(this).toString()
}

class MetadataAction @Inject() (parser: BodyParsers.Default)(implicit
    ec: ExecutionContext,
    config: Configuration
) extends ActionBuilderImpl(parser) {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val otSettings: OTSettings = loadConfigurationObject[OTSettings]("ot", config)

  val apiVersion: APIVersion = otSettings.meta.apiVersion
  val dataVersion: DataVersion = otSettings.meta.dataVersion
  val metadataLoggingConfig = otSettings.logging

  val operationFilters: Map[String, Int] = metadataLoggingConfig.ignoredQueries.map(_ -> 1).toMap

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    logger.trace("Invoking metadata action to generate query time and argument log data.")

    val startTime = System.currentTimeMillis()
    val result: Future[Result] = block(request)

    result.map { response =>
      val responseHeaders = response.header.headers
      val opHeader = responseHeaders.get(GQL_OP_HEADER)
      val r = opHeader match {
        case Some(operation) =>
          if (operationFilters.contains(operation))
            response
          else {

            val endTime = System.currentTimeMillis
            val query = request.body.asInstanceOf[JsObject].value("query")
            // replace consecutive white spaces with single white space to make it easier to read
            val trimmedQuery = query
              .toString()
              .replaceAll("\\s+", " ")
            val requestTime = endTime - startTime

            val meta = GqlRequestMetadata(
              request.headers.hasHeader(metadataLoggingConfig.otHeader),
              new java.sql.Timestamp(System.currentTimeMillis()),
              requestTime,
              responseHeaders.getOrElse(GQL_OP_HEADER, ""),
              responseHeaders.getOrElse(GQL_VAR_HEADER, ""),
              responseHeaders.getOrElse(GQL_COMPLEXITY_HEADER, ""),
              trimmedQuery,
              apiVersion,
              dataVersion
            )

            logger.info(meta.toString) // TODO: review logger

            response
          }
        case None => response
      }
      r.discardingHeader(GQL_OP_HEADER).discardingHeader(GQL_VAR_HEADER)
    }
  }
}
