package utils.prometheus

import io.prometheus.metrics.config.PrometheusProperties
import io.prometheus.metrics.instrumentation.jvm.*
import io.prometheus.metrics.model.registry.PrometheusRegistry

import java.util
import java.util.concurrent.ConcurrentHashMap

class CustomJvmMetrics

/** Custom implementation of JvmMetrics to exclude jvm_runtime_info due to compatibility issues with
  * Prometheus
  */
object CustomJvmMetrics {
  private val REGISTERED = ConcurrentHashMap.newKeySet

  def builder() = CustomJvmMetrics.Builder(PrometheusProperties.get)

  def builder(config: PrometheusProperties) = CustomJvmMetrics.Builder(config)

  class Builder(val config: PrometheusProperties) {

    def register(): Unit =
      register(PrometheusRegistry.defaultRegistry)

    def register(registry: PrometheusRegistry): Unit =
      if (REGISTERED.add(registry)) {
        JvmThreadsMetrics.builder(config).register(registry)
        JvmBufferPoolMetrics.builder(config).register(registry)
        JvmClassLoadingMetrics.builder(config).register(registry)
        JvmCompilationMetrics.builder(config).register(registry)
        JvmGarbageCollectorMetrics.builder(config).register(registry)
        JvmMemoryPoolAllocationMetrics.builder(config).register(registry)
        JvmMemoryMetrics.builder(config).register(registry)
        JvmNativeMemoryMetrics.builder(config).register(registry)
        ProcessMetrics.builder(config).register(registry)
      }
  }
}
