package controllers.api.v4.rest

import javax.inject.Inject
import models.gql.Fetchers
import net.logstash.logback.argument.StructuredArguments.keyValue
import play.api.cache.AsyncCacheApi
import play.api.mvc.*
import services.ApplicationStart
import utils.OTLogging

import scala.concurrent.{ExecutionContext, Future}

class CacheController @Inject() (implicit
    ec: ExecutionContext,
    cc: ControllerComponents,
    restHelpers: RestHelpers,
    gqlQueryCache: AsyncCacheApi,
    appStart: ApplicationStart
) extends AbstractController(cc)
    with OTLogging {

  def clearCache(): Action[AnyContent] =
    restHelpers.checkCredentials(Action.async { request =>
      appStart.RequestCounter.labelValues("/api/v4/rest/cache/clear", "GET").inc()
      Future {
        logger.info(
          s"received request to clear cache from ip ${request.connection.remoteAddressString}",
          keyValue("request.method", request.method)
        )
        Fetchers.resetCache()
        gqlQueryCache.removeAll()
        Ok("cache cleared")
      }
    })

}
