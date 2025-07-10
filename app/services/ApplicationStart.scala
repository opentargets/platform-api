package services

import com.google.inject.Singleton
import io.prometheus.metrics.core.metrics.{Counter, Histogram}
import io.prometheus.metrics.model.snapshots.Unit
import utils.prometheus.CustomJvmMetrics

import javax.inject.Inject

@Singleton
class ApplicationStart @Inject() (
    // Add your dependencies here
) {

  CustomJvmMetrics.builder().register()

  // TODO: Register prometheus metrics
  val RequestCounter: Counter = Counter
    .builder()
    .name("platform_api_http_requests_total")
    .help("total number of http requests")
    .labelNames("handler", "method", "query_name")
    .register

  val QueryTime: Histogram = Histogram
    .builder()
    .name("platform_api_graphql_query_time")
    .help("histogram measuring the time taken for query execution")
    .unit(Unit.SECONDS)
    .register();

  val FieldUsageCount: Counter = Counter
    .builder()
    .name("platform_api_graphql_field_usage")
    .help("count the usages of different fields")
    .labelNames("field_name")
    .register()

  val FieldErrorCount: Counter = Counter
    .builder()
    .name("platform_api_graphql_field_error")
    .help("count the number of times that processing has failed on a given field")
    .labelNames("field_name")
    .register()

}
