package controllers.api.v4.rest

import javax.inject._
import models.Backend
import models.entities.Pagination
import models.entities.SearchResult.JSONImplicits._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class AltSearchController @Inject()(implicit ec: ExecutionContext, backend: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {

  def altSearch(q: String) = Action.async { req =>
    for {
      res <- backend.altSearch(q, None)
    } yield Ok(Json.toJson(res))
  }
}
