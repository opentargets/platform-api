package controllers.api.v4.graphql

import controllers.filters.QueryMetadataLogger.{GQL_OP_HEADER, GQL_VAR_HEADER}

import javax.inject._
import models.entities.TooComplexQueryError
import models.{Backend, GQLSchema}
import play.api.libs.json._
import sangria.execution._
import sangria.marshalling.playJson._
import sangria.parser.{QueryParser, SyntaxError}

import scala.concurrent._
import scala.util.{Failure, Success}
import models.entities.TooComplexQueryError._
import play.api.Logging
import play.api.mvc._


@Singleton
class GraphQLController @Inject()(implicit
                                  ec: ExecutionContext,
                                  dbTables: Backend,
                                  cc: ControllerComponents,
                                  metadataAction: MetadataAction
                                 ) extends AbstractController(cc) with Logging {

  def options: Action[AnyContent] = Action {
    NoContent
  }

  def gql(query: String, variables: Option[String], operation: Option[String]): Action[AnyContent] =
    Action.async {
      executeQuery(query, variables map parseVariables, operation)
    }

  def gqlBody(): Action[JsValue] = metadataAction(parse.json).async { request =>
    val query = (request.body \ "query").as[String]
    val operation = (request.body \ "operationName").asOpt[String]

    val variables = (request.body \ "variables").toOption.flatMap {
      case JsString(vars) => Some(parseVariables(vars))
      case obj: JsObject => Some(obj)
      case _ => None
    }

    executeQuery(query, variables, operation)
  }

  private def parseVariables(variables: String) =
    if (variables.trim == "" || variables.trim == "null") Json.obj()
    else Json.parse(variables).as[JsObject]

  private def executeQuery(query: String, variables: Option[JsObject], operation: Option[String]): Future[Result] =
    QueryParser.parse(query) match {

      // query parsed successfully, time to execute it!
      case Success(queryAst) =>
        Executor
          .execute(
            GQLSchema.schema,
            queryAst,
            dbTables,
            operationName = operation,
            variables = variables getOrElse Json.obj(),
            deferredResolver = GQLSchema.resolvers,
            exceptionHandler = exceptionHandler,
            queryReducers = List(
              QueryReducer.rejectMaxDepth[Backend](15),
              QueryReducer.rejectComplexQueries[Backend](4000, (_, _) => TooComplexQueryError)
            )
          )
          .map(Ok(_)
            .withHeaders(
              (GQL_OP_HEADER, queryAst.operation().get.name.getOrElse("Unknown")),
              (GQL_VAR_HEADER, variables.getOrElse(Json.obj()).toString())
            ))
          .recover {
            case error: QueryAnalysisError => BadRequest(error.resolveError)
            case error: ErrorWithResolver => {
              logger.error(error.getMessage)
              InternalServerError(error.resolveError)
            }
          }

      // can't parse GraphQL query, return error
      case Failure(error: SyntaxError) =>
        Future.successful(
          BadRequest(
            Json.obj(
              "syntaxError" -> error.getMessage,
              "locations" -> Json.arr(
                Json.obj(
                  "line" -> error.originalError.position.line,
                  "column" -> error.originalError.position.column
                )
              )
            )
          )
        )

      case Failure(error) =>
        throw error
    }
}
