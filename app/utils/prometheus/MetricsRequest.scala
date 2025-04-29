package controllers.api.v4.rest

import io.prometheus.metrics.exporter.common.PrometheusHttpRequest
import play.api.mvc.{AnyContent, Request}
import scala.jdk.CollectionConverters._

import java.util

case class MetricsRequest(request: Request[AnyContent]) extends PrometheusHttpRequest {

  override def getQueryString: String = request.rawQueryString

  override def getHeaders(name: String): util.Enumeration[String] =
    val headers = request.headers.get(name)

    headers match
      case Some(header) => Seq(header).iterator.asJavaEnumeration
      case None         => util.Collections.emptyEnumeration()

  override def getMethod: String = request.method

  override def getRequestPath: String = request.path
}
