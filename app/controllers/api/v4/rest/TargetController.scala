package controllers.api.v4.rest

import javax.inject._
import models.Entities.JSONImplicits._
import models.Entities._
import models.entities._
import models.entities.APIErrorMessage.JSONImplicits._
import models.entities.Target.JSONImplicits._
import models.{Backend, GQLSchema}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import sangria.macros._
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError, QueryReducer}
import sangria.execution.deferred.FetcherContext
import sangria.marshalling.playJson._
import sangria.marshalling.InputUnmarshaller

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TargetController @Inject()(implicit ec: ExecutionContext, backend: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {
  val logger = Logger(this.getClass)

  val targetsGQLQ =
    gql"""
        query targetsQuery($$ids: [String!]!) {
          targets(ensemblIds: $$ids) {
            id
            approvedSymbol
            approvedName
            bioType
            hgncId
            nameSynonyms
            symbolSynonyms
            genomicLocation {
              chromosome
              start
              end
              strand
            }
            proteinAnnotations {
              id
              accessions
              functions
            }
          }
        }
      """

  val targetGQLQ =
    gql"""
        query targetQuery($$id: String!) {
          target(ensemblId: $$id) {
            id
            approvedSymbol
            approvedName
            bioType
            hgncId
            nameSynonyms
            symbolSynonyms
            genomicLocation {
              chromosome
              start
              end
              strand
            }
            proteinAnnotations {
              id
              accessions
              functions
            }
          }
        }
      """

  private def queryTargets(targetIds: Seq[String]) = {
    logger.debug(s"parsed document: ${targetsGQLQ.renderPretty}")

    Executor.execute(GQLSchema.schema, targetsGQLQ, backend,
      variables = InputUnmarshaller.mapVars(Map("ids" -> targetIds)),
      deferredResolver = GQLSchema.resolvers,
      exceptionHandler = exceptionHandler,
      queryReducers = List(
        QueryReducer.rejectMaxDepth[Backend](15),
        QueryReducer.rejectComplexQueries[Backend](4000, (_, _) => TooComplexQueryError)))
      .map(Ok(_))
      .recover {
        case error: QueryAnalysisError => BadRequest(error.resolveError)
        case error: ErrorWithResolver => InternalServerError(error.resolveError)
      }
  }

  private def queryTarget(targetId: String) = {
    logger.debug(s"parsed document: ${targetGQLQ.renderPretty}")

    Executor.execute(GQLSchema.schema, targetGQLQ, backend,
      variables = InputUnmarshaller.mapVars(Map("id" -> targetId)),
      deferredResolver = GQLSchema.resolvers,
      exceptionHandler = exceptionHandler,
      queryReducers = List(
        QueryReducer.rejectMaxDepth[Backend](15),
        QueryReducer.rejectComplexQueries[Backend](4000, (_, _) => TooComplexQueryError)))
      .map(Ok(_))
      .recover {
        case error: QueryAnalysisError => BadRequest(error.resolveError)
        case error: ErrorWithResolver => InternalServerError(error.resolveError)
      }
  }

  // example from here https://github.com/nemoo/play-slick3-example/blob/master/app/controllers/Application.scala
  def byId(id:String) = Action.async { _ =>
//    for {
//      targets <- GQLSchema.targetsFetcher.fetch(ctxT, Seq(id))
//    } yield targets.headOption match {
//      case None => NotFound(Json.toJson(APIErrorMessage(NOT_FOUND, s"$id not found")))
//      case Some(t) => Ok(Json.toJson(t))
//    }
    queryTarget(id)
  }

  def byIds(ids: Seq[String]) = Action.async { req =>
    (req.method, ids) match {
      case ("POST", _ :: _) | ("GET", _ :: _) =>
        queryTargets(ids)

      case ("POST", Nil) =>
        req.body.asJson.map(_.as[TargetsBody]) match {
          case Some(body) =>
            queryTargets(body.ids)

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
