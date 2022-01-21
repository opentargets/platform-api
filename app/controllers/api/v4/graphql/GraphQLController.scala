package controllers.api.v4.graphql

import akka.stream.{ActorMaterializer, Materializer}
import controllers.api.v4.graphql.QueryMetadataHeaders.{GQL_OP_HEADER, GQL_VAR_HEADER}
import models.entities.TooComplexQueryError
import models.entities.TooComplexQueryError._
import models.{Backend, GQLSchema}
import org.apache.http.HttpStatus
import play.api.Logging
import play.api.cache.AsyncCacheApi
import play.api.libs.json._
import play.api.mvc._
import sangria.execution._
import sangria.marshalling.playJson._
import sangria.parser.{QueryParser, SyntaxError}

import javax.inject._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class GqlQuery(query: String, variables: JsObject, operation: Option[String])

@Singleton
class GraphQLController @Inject()(implicit
                                  ec: ExecutionContext,
                                  mat: Materializer,
                                  dbTables: Backend,
                                  cache: AsyncCacheApi,
                                  cc: ControllerComponents,
                                  metadataAction: MetadataAction)
    extends AbstractController(cc)
    with Logging {

  private val non200CacheDuration = Duration(10, "seconds")

  def options: Action[AnyContent] = Action {
    NoContent
  }

  def gql(query: String, variables: Option[String], operation: Option[String]): Action[AnyContent] =
    metadataAction.async {
      cachedQuery(GqlQuery(query, (variables map parseVariables).getOrElse(Json.obj()), operation))
    }

  def gqlBody(): Action[JsValue] = metadataAction(parse.json).async { request =>
    val query = (request.body \ "query").as[String]
    val operation = (request.body \ "operationName").asOpt[String]

    val variables: JsObject = (request.body \ "variables").toOption
      .map {
        case JsString(vars) => parseVariables(vars)
        case obj: JsObject  => obj
        case _              => Json.obj()
      }
      .getOrElse(Json.obj())

    cachedQuery(GqlQuery(query, variables, operation))
  }

  private def parseVariables(variables: String) =
    if (variables.trim == "" || variables.trim == "null") Json.obj()
    else Json.parse(variables).as[JsObject]

  private def responseContainsErrors(response: Result): Future[Boolean] =
    response.body.consumeData
      .map(_.utf8String)
      .map(Json.parse)
      .map { json =>
        (json \ "errors").isDefined
      }

  private def cachedQuery(gqlQuery: GqlQuery): Future[Result] = {
    def cacheable(op: Option[String]): Boolean = !op.contains("IntrospectionQuery")

    if (cacheable(gqlQuery.operation)) {
      val fromCache: Future[Option[Result]] = cache.get[Result](gqlQuery.toString)
      val cacheResult: Future[Result] = fromCache.flatMap {
        case Some(result) => Future.successful(result)
        case None =>
          logger.debug(s"Cache miss on ${gqlQuery.operation}: ${gqlQuery.variables}")
          val queryResult = executeQuery(gqlQuery)
          queryResult.andThen {
            case Success(s) =>
              if (s.header.status == HttpStatus.SC_OK) {
                /*
                All GraphQL responses which pass basic validation return status code 200. If something went wrong a field
                returned called 'errors'. If there were no errors, this field isn't present.
                 */
                responseContainsErrors(s).onComplete {
                  case Success(hasErrors) =>
                    if (hasErrors) {
                      logger.info(s"Temporarily caching 200 response with errors")
                      cache.set(gqlQuery.toString, s, non200CacheDuration)
                    } else {
                      logger.info(
                        s"Caching 200 response on ${gqlQuery.operation}: ${gqlQuery.query.filter(_ >= ' ')}"
                      )
                      cache.set(gqlQuery.toString, s)
                    }
                  case Failure(exception) => logger.error(exception.getMessage)
                }
              }
          }
      }
      cacheResult
    } else {
      executeQuery(gqlQuery)
    }

  }

  private def executeQuery(
      gqlQuery: GqlQuery
  ): Future[Result] =
    QueryParser.parse(gqlQuery.query) match {

      // query parsed successfully, time to execute it!
      case Success(queryAst) =>
        Executor
          .execute(
            GQLSchema.schema,
            queryAst,
            dbTables,
            operationName = gqlQuery.operation,
            variables = gqlQuery.variables,
            deferredResolver = GQLSchema.resolvers,
            exceptionHandler = exceptionHandler,
            queryReducers = List(
              QueryReducer.rejectMaxDepth[Backend](15),
              QueryReducer.rejectComplexQueries[Backend](4000, (_, _) => TooComplexQueryError)
            )
          )
          .map(
            Ok(_)
              .withHeaders(
                (GQL_OP_HEADER,
                 queryAst.operation().map(op => op.name).getOrElse("Unknown").toString),
                (GQL_VAR_HEADER, gqlQuery.variables.toString())
              )
          )
          .recover {
            case error: QueryAnalysisError => BadRequest(error.resolveError)
            case error: ErrorWithResolver =>
              logger.error(error.getMessage)
              InternalServerError(error.resolveError)
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
