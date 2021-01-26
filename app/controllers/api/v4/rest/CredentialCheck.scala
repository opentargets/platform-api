package controllers.api.v4.rest

import com.typesafe.config.Config
import javax.inject.Inject
import play.api.Logging
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class CredentialCheck @Inject()(implicit ec: ExecutionContext,
                                cc: ControllerComponents,
                                config: Config)
  extends AbstractController(cc)
    with Logging {

  def checkCredentials[A](action: Action[A]): Action[A] = Action.async(action.parser) { request =>
    logger.info(s"Checking admin credentials")
    request.headers
      .get("apiKey")
      .collect {
        case key if key.hashCode.toString equals config.getString("ot.apiKeyHash") =>
          action(request)
      }
      .getOrElse {
        logger.warn("No apiKey header provided on request to secure endpoint.")
        Future.successful(Forbidden("Invalid API key (not) provided."))
      }
  }
}
