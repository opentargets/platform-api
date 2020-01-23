package controllers.api.v4.rest

import javax.inject._
import models.Backend
import models.entities.SearchResult.JSONImplicits._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class SearchController @Inject()(implicit ec: ExecutionContext, backend: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {

  // TODO move this to graphql query instead doing its own query
  def search(q: String) = Action.async { req =>
    for {
      res <- backend.search(q, None, Seq.empty)
    } yield Ok(Json.toJson(res))
  }
}
