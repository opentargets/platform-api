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

  val metric_prefix = "platform_api_"

  // TODO: Register prometheus metrics
  val RequestCounter: Counter = Counter
    .builder()
    .name(metric_prefix + "http_requests_total")
    .help("total number of http requests")
    .labelNames("handler", "method", "query_name")
    .register()

  val CacheRegistrationCounter: Counter = Counter
    .builder()
    .name(metric_prefix + "cache_registration_total")
    .help("total number of queries registered in the cache")
    .labelNames("query_name")
    .register()

  val CacheMissedCounter: Counter = Counter
    .builder()
    .name(metric_prefix + "cache_missed_total")
    .help("total number of times queries were not found from the query")
    .labelNames("query_name")
    .register()

  val QueryTime: Histogram = Histogram
    .builder()
    .name(metric_prefix + "graphql_query_time")
    .help("histogram measuring the time taken for query execution")
    .unit(Unit.SECONDS)
    .register();

  val FieldUsageCount: Counter = Counter
    .builder()
    .name(metric_prefix + "graphql_field_usage")
    .help("count the usages of different fields")
    .labelNames("field_name")
    .register()

  val FieldErrorCount: Counter = Counter
    .builder()
    .name(metric_prefix + "graphql_field_error")
    .help("count the number of times that processing has failed on a given field")
    .labelNames("field_name")
    .register()

}
