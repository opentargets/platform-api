package controllers.api.v4.rest

import io.prometheus.metrics.config.PrometheusProperties
import io.prometheus.metrics.expositionformats.ExpositionFormats
import io.prometheus.metrics.model.registry.PrometheusRegistry

import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

case class PrometheusScraper(config: PrometheusProperties = PrometheusProperties.get,
                             registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry
) {

  private val lastResponseSize = new AtomicInteger(2 << 9) //  0.5 MB
  private val expositionFormats: ExpositionFormats =
    ExpositionFormats.init(config.getExporterProperties)

  def scrape(request: MetricsRequest) = {
    val snapshot = registry.scrape(request)
    val responseBuffer = new ByteArrayOutputStream(lastResponseSize.get + 1024)
    val acceptHeader = request.getHeader("Accept")
    val writer = expositionFormats.findWriter(acceptHeader)
    writer.write(responseBuffer, snapshot)
    responseBuffer.flush()
    responseBuffer
  }
}
