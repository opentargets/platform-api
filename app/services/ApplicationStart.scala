package services

import com.google.inject.Singleton
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics

import javax.inject.Inject

@Singleton
class ApplicationStart @Inject() (
    // Add your dependencies here
) {

  JvmMetrics.builder().register()

  //TODO: Register prometheus metrics
  val counter: Counter = Counter
    .builder()
    .name("requests_total")
    .help("total number of requests")
    .labelNames("service").register
}
