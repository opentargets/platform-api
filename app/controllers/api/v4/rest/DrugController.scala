package controllers.api.v4.rest

import javax.inject._
import models.Entities.JSONImplicits._
import models.Entities._
import models.entities._
import models.entities.APIErrorMessage.JSONImplicits._
import models.entities.Drug.JSONImplicits._
import models.entities.Violations.InputParameterCheckError
import models.{Backend, GQLSchema}
import play.api.libs.json._
import play.api.mvc._
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, ExceptionHandler, Executor, HandledException, MaxQueryDepthReachedError, QueryAnalysisError, QueryReducer}
import sangria.execution.deferred.FetcherContext
import sangria.marshalling.playJson._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DrugController @Inject()(implicit ec: ExecutionContext, backend: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {
  lazy val ctxD = FetcherContext(backend,
    GQLSchema.drugsFetcher,
    Some(GQLSchema.drugsFetcherCache),
    Map(GQLSchema.drugsFetcher -> GQLSchema.drugsFetcherCache,
      GQLSchema.targetsFetcher -> GQLSchema.targetsFetcherCache),
    Vector(GQLSchema.drugsFetcher,
      GQLSchema.targetsFetcher))

//  private lazy val exceptionHandler = ExceptionHandler {
//    case (_, error @ TooComplexQueryError) => HandledException(error.getMessage)
//    case (_, error @ MaxQueryDepthReachedError(_)) => HandledException(error.getMessage)
//    case (_, error @ InputParameterCheckError(_)) => HandledException(error.getMessage)
//  }
//  private def queryDrug(drugId: String) = {
//    Executor.execute(GQLSchema.schema, Document() , backend,
//      operationName = Some("query"),
//      variables = Json.obj(),
//      deferredResolver = GQLSchema.resolvers,
//      exceptionHandler = exceptionHandler,
//      queryReducers = List(
//        QueryReducer.rejectMaxDepth[Backend](3),
//        QueryReducer.rejectComplexQueries[Backend](4000, (_, _) => TooComplexQueryError)))
//      .map(Ok(_))
//      .recover {
//        case error: QueryAnalysisError => BadRequest(error.resolveError)
//        case error: ErrorWithResolver => InternalServerError(error.resolveError)
//      }
//  }
  // example from here https://github.com/nemoo/play-slick3-example/blob/master/app/controllers/Application.scala
  def byId(id:String) = Action.async { req =>

    for {
      drugs <- GQLSchema.drugsFetcher.fetch(ctxD, Seq(id))
    } yield drugs.headOption match {
      case None => NotFound(Json.toJson(APIErrorMessage(NOT_FOUND, s"$id not found")))
      case Some(t) => Ok(Json.toJson(t))
    }
  }

  def byIds(ids: Seq[String]) = Action.async { req =>
    (req.method, ids) match {
      case ("POST", _ :: _) | ("GET", _ :: _) =>
        for {
          drugs <- GQLSchema.drugsFetcher.fetch(ctxD, ids)
        } yield Ok(Json.toJson(drugs))

      case ("POST", Nil) =>
        req.body.asJson.map(_.as[TargetsBody]) match {
          case Some(body) =>
            for {
              drugs <- GQLSchema.drugsFetcher.fetch(ctxD, body.ids)
            } yield Ok(Json.toJson(drugs))

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
