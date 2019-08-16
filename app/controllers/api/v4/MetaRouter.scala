package controllers.api.v4

import javax.inject._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import controllers.MetaController

import scala.concurrent.ExecutionContext

@Singleton
class MetaRouter @Inject()(controller: MetaController) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/info") =>
      controller.meta
    case GET(p"/health") =>
      controller.healthcheck()
  }
}
