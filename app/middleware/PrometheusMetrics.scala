package middleware

import com.google.inject.Singleton
import sangria.execution.{
  BeforeFieldResult,
  Middleware,
  MiddlewareAfterField,
  MiddlewareErrorField,
  MiddlewareQueryContext
}
import sangria.schema.Context
import services.ApplicationStart
import io.prometheus.metrics.model.snapshots.Unit as PrometheusUnit

@Singleton
class PrometheusMetrics(implicit appStart: ApplicationStart)
    extends Middleware[Any]
    with MiddlewareAfterField[Any]
    with MiddlewareErrorField[Any] {
  override type QueryVal = Long
  override type FieldVal = Unit

  override def beforeQuery(context: MiddlewareQueryContext[Any, ?, ?]): Long =
    System.nanoTime()

  override def afterQuery(queryVal: Long, context: MiddlewareQueryContext[Any, ?, ?]): Unit =
    appStart.QueryTime.observe(PrometheusUnit.nanosToSeconds(System.nanoTime - queryVal))

  override def beforeField(queryVal: Long,
                           mctx: MiddlewareQueryContext[Any, ?, ?],
                           ctx: Context[Any, ?]
  ): BeforeFieldResult[Any, Unit] = continue

  override def afterField(queryVal: Long,
                          fieldVal: Unit,
                          value: Any,
                          mctx: MiddlewareQueryContext[Any, ?, ?],
                          ctx: Context[Any, ?]
  ): Option[Any] = {
    val fieldName = ctx.parentType.name + "_" + ctx.field.name
    appStart.FieldUsageCount.labelValues(fieldName).inc()
    None
  }

  override def fieldError(queryVal: Long,
                          fieldVal: Unit,
                          error: Throwable,
                          mctx: MiddlewareQueryContext[Any, _, _],
                          ctx: Context[Any, _]
  ): Unit = {
    val fieldName = ctx.parentType.name + "_" + ctx.field.name
    appStart.FieldErrorCount.labelValues(fieldName).inc()
  }
}
