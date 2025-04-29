package controllers.api.v4.graphql

import org.apache.pekko.stream.Materializer
import controllers.api.v4.graphql.QueryMetadataHeaders.{
  GQL_COMPLEXITY_HEADER,
  GQL_OP_HEADER,
  GQL_VAR_HEADER
}
import middleware.PrometheusMetrics
import models.Helpers.loadConfigurationObject
import models.entities.Configuration.OTSettings
import models.entities.TooComplexQueryError
import models.entities.TooComplexQueryError.*
import models.{Backend, GQLSchema}
import org.apache.http.HttpStatus
import play.api.{Configuration, Logging}
import play.api.cache.AsyncCacheApi
import play.api.libs.json.*
import play.api.mvc.*
import sangria.execution.*
import sangria.marshalling.playJson.*
import sangria.parser.{QueryParser, SyntaxError}
import services.ApplicationStart

import java.sql.Timestamp
import javax.inject.*
import scala.concurrent.*
import scala.concurrent.duration.*
import scala.util.{Failure, Success}

case class GqlQuery(query: String, variables: JsObject, operation: Option[String])

@Singleton
class GraphQLController @Inject() (implicit
    ec: ExecutionContext,
    mat: Materializer,
    dbTables: Backend,
    cache: AsyncCacheApi,
    cc: ControllerComponents,
    metadataAction: MetadataAction,
    config: Configuration,
    appStart: ApplicationStart
//                                   prometheusMetricsMiddleware: PrometheusMetrics
) extends AbstractController(cc)
    with Logging {

  implicit val otSettings: OTSettings = loadConfigurationObject[OTSettings]("ot", config)

  private val non200CacheDuration = Duration(10, "seconds")

  def options: Action[AnyContent] = Action {
    NoContent
  }

  def gql(query: String, variables: Option[String], operation: Option[String]): Action[AnyContent] =
    metadataAction.async {
      appStart.RequestCounter.labelValues("/api/v4/graphql", "GET").inc()
      val gqlQuery =
        GqlQuery(query, (variables map parseVariables).getOrElse(Json.obj()), operation)
      runQuery(gqlQuery)
    }

  def gqlBody(): Action[JsValue] = metadataAction(parse.json).async { request =>
    appStart.RequestCounter.labelValues("/api/v4/graphql", "POST").inc()
    val query = (request.body \ "query").as[String]
    val operation = (request.body \ "operationName").asOpt[String]

    val variables: JsObject = (request.body \ "variables").toOption
      .map {
        case JsString(vars) => parseVariables(vars)
        case obj: JsObject  => obj
        case _              => Json.obj()
      }
      .getOrElse(Json.obj())

    val gqlQuery = GqlQuery(query, variables, operation)
    runQuery(gqlQuery)
  }

  private def runQuery(gqlQuery: GqlQuery)(implicit otSettings: OTSettings) =
    if (otSettings.ignoreCache)
      executeQuery(gqlQuery)
    else cachedQuery(gqlQuery)

  private def parseVariables(variables: String) =
    if (variables.trim == "" || variables.trim == "null") Json.obj()
    else Json.parse(variables).as[JsObject]

  private def responseContainsErrors(response: Result): Future[(Boolean, Option[String])] =
    response.body.consumeData
      .map(_.utf8String)
      .map(Json.parse)
      .map { json =>
        val errorsOpt = (json \ "errors").toOption.map(_.toString())
        (errorsOpt.isDefined, errorsOpt)
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
          queryResult.andThen { case Success(s) =>
            if (s.header.status == HttpStatus.SC_OK) {
              /*
                All GraphQL responses which pass basic validation return status code 200. If something went wrong a field
                returned called 'errors'. If there were no errors, this field isn't present.
               */
              responseContainsErrors(s).onComplete {
                case Success((hasErrors, errorMessagesOpt)) =>
                  if (hasErrors) {
                    logger.info(s"Temporarily caching 200 response with errors")
                    errorMessagesOpt.foreach(errors => logger.error(s"Errors in response: $errors"))
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
        var queryComplexity = -1.0
        Executor
          .execute(
            GQLSchema.schema,
            queryAst,
            dbTables,
//            middleware = prometheusMetricsMiddleware :: Nil,
            operationName = gqlQuery.operation,
            variables = gqlQuery.variables,
            deferredResolver = GQLSchema.resolvers,
            exceptionHandler = exceptionHandler,
            queryReducers = List(
              QueryReducer.measureComplexity[Backend] { (c, ctx) =>
                queryComplexity = c
                ctx
              },
              QueryReducer.rejectMaxDepth[Backend](15),
              QueryReducer.rejectComplexQueries[Backend](150000, (_, _) => TooComplexQueryError)
            )
          )
          .map(
            Ok(_)
              .withHeaders(
                (GQL_OP_HEADER,
                 queryAst
                   .operation()
                   .map(op => op.name.getOrElse("Unknown operation"))
                   .getOrElse("Unknown operation")
                ),
                (GQL_VAR_HEADER, gqlQuery.variables.toString()),
                (GQL_COMPLEXITY_HEADER, queryComplexity.toString())
              )
          )
          .recover {
            case error: QueryAnalysisError =>
              val graphQLError: GraphQLError =
                getErrorObject(gqlQuery, queryComplexity, error.getMessage())
              logger.error(graphQLError.toString)
              BadRequest(error.resolveError)
            case error: ErrorWithResolver =>
              val graphQLError: GraphQLError =
                getErrorObject(gqlQuery, queryComplexity, error.getMessage())
              logger.error(graphQLError.toString)
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

  private def getErrorObject(gqlQuery: GqlQuery, queryComplexity: Double, error: String) = {
    val trimmedQuery = gqlQuery.query
      .replaceAll("\\s+", " ")
    val graphQLError = GraphQLError(
      false,
      error,
      new Timestamp(System.currentTimeMillis()),
      gqlQuery.variables.toString(),
      queryComplexity,
      trimmedQuery
    )
    graphQLError
  }
}
