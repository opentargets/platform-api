package controllers.api.v4

import javax.inject._
import models.Backend
import models.entities.SearchResult.JSONImplicits._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class MSearchController @Inject()(implicit ec: ExecutionContext, backend: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {

  def msearch(q: String) = Action.async { req =>
    for {
      res <- backend.msearch(q, None, None)
    } yield Ok(Json.toJson(res))
  }
}
