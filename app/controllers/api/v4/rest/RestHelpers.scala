package controllers.api.v4.rest

import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}

import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class RestHelpers @Inject() (implicit
    ec: ExecutionContext,
    cc: ControllerComponents,
    config: Config
) extends AbstractController(cc) {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def checkCredentials[A](action: Action[A]): Action[A] = Action.async(action.parser) { request =>
    logger.info(s"checking admin credentials")
    request.headers
      .get("apiKey")
      .collect {
        case key if key.hashCode.toString.equals(config.getString("ot.apiKeyHash")) =>
          action(request)
        case _ =>
          logger.error("invalid 'apiKey' provided on request to secure endpoint")
          Future.successful(Forbidden("invalid API key"))
      }
      .getOrElse {
        logger.error("no 'apiKey' header provided on request to secure endpoint.")
        Future.successful(Forbidden("invalid API key (not) provided"))
      }
  }
}
