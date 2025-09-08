package controllers.api.v4.rest

import org.apache.pekko.stream.scaladsl.Source
import play.api.Logging
import play.api.http.HttpEntity.Streamed
import play.api.mvc.{
  AbstractController,
  Action,
  AnyContent,
  ControllerComponents,
  RangeResult,
  ResponseHeader,
  Result
}
import utils.prometheus.{MetricsRequest, PrometheusScraper}

import javax.inject.Inject

class PrometheusController @Inject() (implicit cc: ControllerComponents)
    extends AbstractController(cc)
    with Logging {

  private val prometheusScraper = PrometheusScraper()

  def metrics(): Action[AnyContent] = Action { request =>
    logger.debug("Received request to /metrics")

    implicit val loggerVal = logger

    val metricsRequest = MetricsRequest(request)
    val test = prometheusScraper.scrape(request = metricsRequest)
    val byteStringSource = Source.single(org.apache.pekko.util.ByteString(test.toByteArray))

    Result(
      header = ResponseHeader(200, Map("Content-Type" -> "text/plain")),
      body = Streamed(byteStringSource, None, Some("text/plain"))
    )
  }
}
