package controllers.api.v4

import controllers.TargetController
import javax.inject._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

@Singleton
class TargetRouter @Inject()(controller: TargetController) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/$id") =>
      controller.byId(id)
  }
}
