package controllers.api.v4.rest

import javax.inject.Inject
import models.gql.Fetchers
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
) extends AbstractController(cc)
    with Logging {

  def clearCache(): Action[AnyContent] =
    restHelpers.checkCredentials(Action.async { request =>
      appStart.RequestCounter.labelValues("/api/v4/rest/cache/clear", "GET").inc()
      Future {
        logger.info("Received request to clear cache.")
        Fetchers.resetCache()
        gqlQueryCache.removeAll()
        Ok("Cache cleared.")
      }
    })

}
