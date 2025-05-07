package middleware

import javax.inject.*
import sangria.execution.{BeforeFieldResult, Middleware, MiddlewareAfterField, MiddlewareErrorField, MiddlewareQueryContext}
import sangria.schema.Context
import services.ApplicationStart
import io.prometheus.metrics.model.snapshots.Unit as PrometheusUnit
import middleware.PrometheusMetrics.{excludedFields, excludedQueries}

object PrometheusMetrics {
  val excludedQueries: Set[String] = Set(
    "IntrospectionQuery"
  )
  val excludedFields: Set[String] = Set(
    "__Directive",
    "__EnumValue",
    "__Field",
    "__InputValue",
    "__Schema",
    "__Type"
  )
}

@Singleton
class PrometheusMetrics @Inject() (implicit appStart: ApplicationStart)
    extends Middleware[Any]
    with MiddlewareAfterField[Any]
    with MiddlewareErrorField[Any] {
  override type QueryVal = Long
  override type FieldVal = Unit

  override def beforeQuery(context: MiddlewareQueryContext[Any, ?, ?]): Long =
    context.operationName match
      case Some(name) if excludedQueries.contains(name) => 0L
      case Some(name) => System.nanoTime()
      case None => 0L

  override def afterQuery(queryVal: Long, context: MiddlewareQueryContext[Any, ?, ?]): Unit =
    context.operationName match
      case Some(name) if excludedQueries.contains(name) => ()
      case Some(name) =>
        appStart.QueryTime.observe(PrometheusUnit.nanosToSeconds(System.nanoTime - queryVal))
      case None => ()


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
    if !excludedFields.contains(ctx.parentType.name) then
      val fieldName = ctx.parentType.name + "_" + ctx.field.name
      appStart.FieldUsageCount.labelValues(fieldName).inc()
    None
  }

  override def fieldError(queryVal: Long,
                          fieldVal: Unit,
                          error: Throwable,
                          mctx: MiddlewareQueryContext[Any, ?, ?],
                          ctx: Context[Any, ?]
  ): Unit = {
    if !excludedFields.contains(ctx.parentType.name) then
      val fieldName = ctx.parentType.name + "_" + ctx.field.name
      appStart.FieldErrorCount.labelValues(fieldName).inc()
  }
}


