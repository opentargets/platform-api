package controllers.api.v4.rest

import com.typesafe.config.Config
import javax.inject.Inject
import models.gql.Fetchers
import play.api.Logging
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class CacheController @Inject()(implicit ec: ExecutionContext,
                                cc: ControllerComponents, config: Config)
  extends AbstractController(cc)
    with Logging {

  def clearCache(): Action[AnyContent] = checkCredentials(Action.async { implicit request =>
    Future {
      logger.info("Received request to clear cache.")
      Fetchers.resetCache()
      Ok("Cache cleared.")
    }
  })

  def checkCredentials[A](action: Action[A]): Action[A] = Action.async(action.parser) { request =>
    logger.info(s"Checking admin credentials")
    request.headers
      .get("apiKey")
      .collect {
        case key if key.hashCode.toString equals config.getString("ot.apiKeyHash") => action(request)
      }
      .getOrElse {
        logger.warn("No apiKey header provided on request to secure endpoint.")
        Future.successful(Forbidden("Invalid API key (not) provided."))
      }
  }
}
