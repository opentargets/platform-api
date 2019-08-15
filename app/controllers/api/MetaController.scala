package controllers.api

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

//   def associationsByDisease(id: String, indirect: Boolean, orderBy: Option[String]) = Action.async { request =>
//     for {
//       table <- backend.getAssociationsByDisease(id, indirect, Some(backend.defaultHS), orderBy, None)
//     } yield Ok(views.html.associations(id, table))
//   }
// 
//   def associationsByTarget(id: String, indirect: Boolean, orderBy: Option[String]) = Action.async { request =>
//     for {
//       table <- backend.getAssociationsByTarget(id, indirect, Some(backend.defaultHS), orderBy, None)
//     } yield Ok(views.html.associations(id, table))
//   }

  def healthcheck() = Action { _ =>
    Ok("alive!")
  }
}
