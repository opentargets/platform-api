package controllers.api.v4.rest

import javax.inject._
import models.{Backend, GQLSchema}
import models.Entities._
import models.entities._
import models.Entities.TooComplexQueryError
import models.entities.SearchResult.JSONImplicits._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import sangria.macros._
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError, QueryReducer}
import sangria.execution.deferred.FetcherContext
import sangria.marshalling.playJson._
import sangria.marshalling.InputUnmarshaller

import scala.concurrent.ExecutionContext

@Singleton
class SearchController @Inject()(implicit ec: ExecutionContext, backend: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {
  val logger = Logger(this.getClass)
  val searchGQLQ =
    gql"""
          query searchQuery($$id: String!) {
          search(queryString: $$id) {
            total
            aggregations {
              total
              entities {
                name
                total
                categories {
                  name
                  total
                }
              }
            }
            hits {
              id
              name
              object {
                ... on Target {
                  id
                  approvedSymbol
                  approvedName
                  nameSynonyms
                }
                ... on Disease {
                  id
                  name
                  description
                  synonyms
                  therapeuticAreas {
                    id
                    name
                  }
                }
                ... on Drug {
                  id
                  name
                  synonyms
                  tradeNames
                  yearOfFirstApproval
                  drugType
                }
              }
            }
          }
        }
      """

  private def queryFn(q: String) = {
    logger.debug(s"parsed document: ${searchGQLQ.renderPretty}")

    Executor.execute(GQLSchema.schema, searchGQLQ, backend,
      variables = InputUnmarshaller.mapVars(Map("id" -> q)),
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

  def search(q: String) = Action.async { req =>
    queryFn(q)
//    for {
//      res <- queryFn(q)
//    } yield Ok(Json.toJson(res))
  }
}
