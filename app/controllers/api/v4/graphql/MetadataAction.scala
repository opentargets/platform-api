package controllers.api.v4.graphql

import QueryMetadataHeaders.{GQL_OP_HEADER, GQL_OT_HEADER, GQL_VAR_HEADER}
import models.Backend
import models.entities.Configuration.{APIVersion, DataVersion}
import play.api.Logging
import play.api.mvc.{ActionBuilderImpl, BodyParsers, Request, Result}

import java.sql.Timestamp
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class GqlRequestMetadata(isOT: Boolean, date: Timestamp, duration: Long, operation: Option[String], variables: Option[String], api: APIVersion, data: DataVersion)

class MetadataAction @Inject()(parser: BodyParsers.Default)(implicit ec: ExecutionContext, backend: Backend)
  extends ActionBuilderImpl(parser)
    with Logging {

  val apiVersion: APIVersion = backend.getMeta.apiVersion
  val dataVersion: DataVersion = backend.getMeta.dataVersion

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    logger.trace("Invoking metadata action to generate query time and argument log data.")

    val startTime = System.currentTimeMillis()
    val result: Future[Result] = block(request)

    result.map(response => {
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime
      val responseHeaders = response.header.headers
      val meta = GqlRequestMetadata(
        request.headers.hasHeader(GQL_OT_HEADER),
        new java.sql.Timestamp(System.currentTimeMillis()),
        requestTime,
        responseHeaders.get(GQL_OP_HEADER),
        responseHeaders.get(GQL_VAR_HEADER),
        apiVersion,
        dataVersion
      )

      logger.info(meta.toString)

      response.discardingHeader(GQL_OP_HEADER).discardingHeader(GQL_VAR_HEADER)
    })
  }
}
