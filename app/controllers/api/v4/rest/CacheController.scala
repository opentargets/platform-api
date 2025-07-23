package controllers.api.v4.rest

import javax.inject.Inject
import models.gql.Fetchers
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.{Logger, LoggerFactory}
import play.api.Logging
import play.api.cache.AsyncCacheApi
import play.api.mvc.*
import services.ApplicationStart

import scala.concurrent.{ExecutionContext, Future}

class CacheController @Inject() (implicit
    ec: ExecutionContext,
    cc: ControllerComponents,
    restHelpers: RestHelpers,
    gqlQueryCache: AsyncCacheApi,
    appStart: ApplicationStart
) extends AbstractController(cc) {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def clearCache(): Action[AnyContent] =
    restHelpers.checkCredentials(Action.async { request =>
      appStart.RequestCounter.labelValues("/api/v4/rest/cache/clear", "GET").inc()
      Future {
        logger.info("received request to clear cache", kv("request.method", request.method), kv("request.ip", request.connection.remoteAddressString))
        Fetchers.resetCache()
        gqlQueryCache.removeAll()
        Ok("cache cleared")
      }
    })

}
