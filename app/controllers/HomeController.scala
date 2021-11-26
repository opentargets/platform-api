package controllers

import javax.inject._
import models.Backend
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(implicit ec: ExecutionContext,
                               backend: Backend,
                               cc: ControllerComponents)
    extends AbstractController(cc) {

  // example from here https://github.com/nemoo/play-slick3-example/blob/master/app/controllers/Application.scala
  def index: Action[AnyContent] = Action { _ =>
    Ok(views.html.index(backend.getMeta))
  }
}
