package controllers.api.v4.rest

import javax.inject.*
import models.entities.TooComplexQueryError
import models.entities.TooComplexQueryError.*
import models.{Backend, GQLSchema}
import play.api.mvc.*
import sangria.macros.*
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError, QueryReducer}
import sangria.marshalling.playJson.*
import sangria.marshalling.InputUnmarshaller

import scala.concurrent.ExecutionContext
import sangria.ast.Document
import utils.OTLogging

@Singleton
class MetaController @Inject() (implicit
    ec: ExecutionContext,
    backend: Backend,
    cc: ControllerComponents
) extends AbstractController(cc)
    with OTLogging {

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
    logger.debug(s"parsed document: ${metaGQLQ}")

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
        case error: QueryAnalysisError =>
          logger.error("QueryAnalysisError: " + error.getMessage())
          BadRequest(error.resolveError)
        case error: ErrorWithResolver =>
          logger.error("ErrorWithResolver: " + error.getMessage())
          InternalServerError(error.resolveError)
      }
  }

  // example from here https://github.com/nemoo/play-slick3-example/blob/master/app/controllers/Application.scala
  def meta: Action[AnyContent] = Action.async { _ =>
    queryMeta
  }

  def healthcheck(): Action[AnyContent] = Action { _ =>
    Ok("OK")
  }
}
