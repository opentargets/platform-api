package controllers

import javax.inject._
import models.Backend
import models.Entities.JSONImplicits._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class TargetController @Inject()(implicit ec: ExecutionContext, backend: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {

  // example from here https://github.com/nemoo/play-slick3-example/blob/master/app/controllers/Application.scala
  def byId(id: String) = Action.async { req =>
    for {
      target <- backend.getTargets(Seq(id))
    } yield Ok(Json.toJson(target))
  }
}
