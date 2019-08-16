package controllers.api.v4

import javax.inject._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import scala.concurrent.ExecutionContext

/** currently not used as it is a bit of a pain with path extractor as it needs more work around it */
@Singleton
class MetaRouter @Inject()(controller: MetaController) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/info") =>
      controller.meta
    case GET(p"/health") =>
      controller.healthcheck()
  }
}
