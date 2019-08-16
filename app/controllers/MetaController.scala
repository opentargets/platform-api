package controllers

import javax.inject._
import models.Backend
import models.Entities.JSONImplicits._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class MetaController @Inject()(implicit ec: ExecutionContext, backend: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {

  // example from here https://github.com/nemoo/play-slick3-example/blob/master/app/controllers/Application.scala
  def meta = Action { _ =>
    Ok(Json.toJson(backend.getMeta))
  }

  def healthcheck() = Action { _ =>
    Ok(Json.toJson(backend.getStatus(true)))
  }
}
