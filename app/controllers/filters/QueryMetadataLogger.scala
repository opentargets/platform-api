package controllers.filters

import akka.stream.Materializer
import controllers.filters.QueryMetadataLogger.{GQL_OP_HEADER, GQL_OT_HEADER, GQL_VAR_HEADER}
import play.api.Logging
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object QueryMetadataLogger {
  val GQL_OT_HEADER = "OT-Platform"
  val GQL_OP_HEADER = "gql-operation"
  val GQL_VAR_HEADER = "gql-variables"
}

class QueryMetadataLogger @Inject()(implicit val mat: Materializer, ec: ExecutionContext)
  extends Filter
    with Logging {
  def apply(nextFilter: RequestHeader => Future[Result])(
    requestHeader: RequestHeader): Future[Result] = {
    if (requestHeader.headers.hasHeader(GQL_OT_HEADER)) {
      val startTime = System.currentTimeMillis

      nextFilter(requestHeader).map { result =>
        val endTime = System.currentTimeMillis
        val requestTime = endTime - startTime

        val resultHeader = result.header
        if (resultHeader.headers.contains(GQL_OP_HEADER)) {
          logger.info(
            s"${resultHeader.headers(GQL_OP_HEADER)}: ${resultHeader.headers(GQL_VAR_HEADER)} took ${requestTime}ms and returned ${resultHeader.status}"
          )
          result.withHeaders("Request-Time" -> requestTime.toString)
            .discardingHeader(GQL_OP_HEADER)
            .discardingHeader(GQL_VAR_HEADER)
        }
        else {
          result
        }
      }
    } else {
      logger.debug("Non-FE query received.")
      nextFilter(requestHeader)
    }
  }
}
