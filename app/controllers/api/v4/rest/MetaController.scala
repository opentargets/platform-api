package controllers.api.v4.rest

import javax.inject._
import models.entities.TooComplexQueryError
import models.entities.TooComplexQueryError._
import models.{Backend, GQLSchema}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import sangria.macros._
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError, QueryReducer}
import sangria.marshalling.playJson._
import sangria.marshalling.InputUnmarshaller

import scala.concurrent.ExecutionContext
import sangria.ast.Document

@Singleton
class MetaController @Inject()(implicit
                               ec: ExecutionContext,
                               backend: Backend,
                               cc: ControllerComponents)
    extends AbstractController(cc)
    with Logging {

  val metaGQLQ: Document =
    gql"""
        query metaQuery {
          meta {
            name
            apiVersion {
              x
              y
              z
            }
            dataVersion {
              year
              month
              iteration
            }
          }
        }
      """

  private def queryMeta = {
    logger.debug(s"parsed document: ${metaGQLQ.renderPretty}")

    Executor
      .execute(
        GQLSchema.schema,
        metaGQLQ,
        backend,
        variables = InputUnmarshaller.mapVars(Map.empty[String, Any]),
        deferredResolver = GQLSchema.resolvers,
        exceptionHandler = exceptionHandler,
        queryReducers = List(
          QueryReducer.rejectMaxDepth[Backend](15),
          QueryReducer.rejectComplexQueries[Backend](4000, (_, _) => TooComplexQueryError)
        )
      )
      .map(Ok(_))
      .recover {
        case error: QueryAnalysisError => BadRequest(error.resolveError)
        case error: ErrorWithResolver  => InternalServerError(error.resolveError)
      }
  }

  // example from here https://github.com/nemoo/play-slick3-example/blob/master/app/controllers/Application.scala
  def meta: Action[AnyContent] = Action.async { _ =>
    queryMeta
  }

  def healthcheck(): Action[AnyContent] = Action { _ =>
    Ok(Json.toJson(backend.getStatus(true)))
  }
}
