package controllers.api.v4.rest

import javax.inject._
import models.Entities.JSONImplicits._
import models.Entities.TargetsBody
import models.entities.APIErrorMessage
import models.entities.Target.JSONImplicits._
import models.{Backend, GQLSchema}
import play.api.libs.json._
import play.api.mvc._
import sangria.execution.deferred.FetcherContext

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TargetController @Inject()(implicit ec: ExecutionContext, backend: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {

  lazy val ctxT = FetcherContext(backend,
    GQLSchema.targetsFetcher,
    Some(GQLSchema.targetsFetcherCache), Map.empty, Vector.empty)

  // example from here https://github.com/nemoo/play-slick3-example/blob/master/app/controllers/Application.scala
  def byId(id:String) = Action.async { req =>
    for {
//      targets <- backend.getTargets(Seq(id))
      targets <- GQLSchema.targetsFetcher.fetch(ctxT, Seq(id))
    } yield targets.headOption match {
      case None => NotFound(Json.toJson(APIErrorMessage(NOT_FOUND, s"$id not found")))
      case Some(t) => Ok(Json.toJson(t))
    }
  }

  def byIds(ids: Seq[String]) = Action.async { req =>
    (req.method, ids) match {
      case ("POST", _ :: _) | ("GET", _ :: _) =>
        for {
          targets <- GQLSchema.targetsFetcher.fetch(ctxT, ids)
        } yield Ok(Json.toJson(targets))

      case ("POST", Nil) =>
        req.body.asJson.map(_.as[TargetsBody]) match {
          case Some(body) =>
            for {
              targets <- GQLSchema.targetsFetcher.fetch(ctxT, body.ids)
            } yield Ok(Json.toJson(targets))

          case None => Future.successful(
            BadRequest(Json.toJson(APIErrorMessage(BAD_REQUEST,
              s"body field `ids` must exist as a list of target ids"))))
        }

      case (_, _) =>
        Future.successful(
          BadRequest(Json.toJson(APIErrorMessage(BAD_REQUEST,
            s"parameter `ids` must contain at least one ensembl ID"))))
    }
  }
}
