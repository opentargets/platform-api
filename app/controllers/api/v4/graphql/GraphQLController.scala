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
import play.api.{Configuration, Logging, MarkerContext}
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
import net.logstash.logback.marker.Markers.*

import java.util.UUID

case class GqlQuery(query: String, variables: JsObject, operation: Option[String])(implicit
    markerContext: MarkerContext
)

@Singleton
class GraphQLController @Inject() (implicit
    ec: ExecutionContext,
    mat: Materializer,
    dbTables: Backend,
    cache: AsyncCacheApi,
    cc: ControllerComponents,
    metadataAction: MetadataAction,
    config: Configuration,
    appStart: ApplicationStart,
    prometheusMetricsMiddleware: PrometheusMetrics
) extends AbstractController(cc)
    with Logging {

  implicit val otSettings: OTSettings = loadConfigurationObject[OTSettings]("ot", config)

  private val non200CacheDuration = Duration(10, "seconds")

  def options: Action[AnyContent] = Action {
    NoContent
  }

  private def logRequestReceived(operation: Option[String], request: Request[Any])(implicit
      markerContext: MarkerContext
  ): Unit =
    val origin = request.headers.get("Origin").getOrElse("unknown").split("://").last
    val domain = request.domain
    // Validate origin to check if request comes from webapp
    val isOT = request.headers.get("X-Correlation-Id").isDefined

    val ip = request.headers.get("X-Forwarded-For") match {
      case Some(xff) => xff.split(",").head.trim
      case None      => request.connection.remoteAddressString
    }

    import utils.logging.LogUtils.*
    val newMarkers = markerContext.fromExistingContext(
      append("request.method", request.method).and(append("isOT", isOT))
    )

    operation match {
      case None =>
        logger.info(s"request received from ip $ip with no operation name")(newMarkers)
      case Some(op) =>
        if (op != "IntrospectionQuery")
          logger.info(
            s"request received from ip $ip"
          )(newMarkers)
    }

  /** Adds a request id value to the logging context so that all logs for the same request have this
    * id. This id can later be used to correlate the log messages. If the request headers contain
    * `request-id` this value will be used if not a GUID will be generated
    * @param request
    *   HTTP request.
    */
  private def getRequestIdToLoggingContext(request: Request[Any]): String =
    val requestId = request.headers.get("X-Correlation-Id") match
      case Some(id) => id
      case None     => UUID.randomUUID().toString

    requestId

  // request.connection.remoteAddress.getHostAddress
  def gql(query: String, variables: Option[String], operation: Option[String]): Action[AnyContent] =
    metadataAction.async { request =>
      appStart.RequestCounter.labelValues("/api/v4/graphql", "GET").inc()
      appStart.GraphqlRequestCounter.labelValues("GET", operation.getOrElse("")).inc()

      val correlationId = getRequestIdToLoggingContext(request)

      implicit val correlationMarker: MarkerContext =
        MarkerContext(append("correlation.id", correlationId))
      dbTables.setMarkerContext(correlationMarker)

      logRequestReceived(operation, request)

      val gqlQuery =
        GqlQuery(query, (variables map parseVariables).getOrElse(Json.obj()), operation)

      runQuery(gqlQuery)
    }

  def gqlBody(): Action[JsValue] = metadataAction(parse.json).async { request =>
    val correlationId = getRequestIdToLoggingContext(request)

    implicit val correlationMarker: MarkerContext =
      MarkerContext(append("correlation.id", correlationId))
    dbTables.setMarkerContext(correlationMarker)

    val query = (request.body \ "query").as[String]
    val operation = (request.body \ "operationName").asOpt[String]
    appStart.RequestCounter.labelValues("/api/v4/graphql", "POST").inc()
    appStart.GraphqlRequestCounter.labelValues("POST", operation.getOrElse("")).inc()

    logRequestReceived(operation, request)

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

  private def runQuery(
      gqlQuery: GqlQuery
  )(implicit otSettings: OTSettings, markerContext: MarkerContext) =
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

  private def cachedQuery(
      gqlQuery: GqlQuery
  )(implicit markerContext: MarkerContext): Future[Result] = {
    def cacheable(op: Option[String]): Boolean = !op.contains("IntrospectionQuery")

    if (cacheable(gqlQuery.operation)) {
      val fromCache: Future[Option[Result]] = cache.get[Result](gqlQuery.toString)
      val cacheResult: Future[Result] = fromCache.flatMap {
        case Some(result) => Future.successful(result)
        case None =>
          logger.info(s"cache miss: ${gqlQuery.variables}")(
            MarkerContext(append("operation", gqlQuery.operation))
          )
          appStart.CacheMissedCounter.labelValues(gqlQuery.operation.getOrElse("")).inc()
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
                    logger.info(s"temporarily caching 200 response with errors")(
                      MarkerContext(append("operation", gqlQuery.operation))
                    )
                    errorMessagesOpt.foreach(errors => logger.error(s"errors in response: $errors"))
                    cache.set(gqlQuery.toString, s, non200CacheDuration)
                  } else {
                    logger.info(
                      s"caching 200 response: ${gqlQuery.query.filter(_ >= ' ')}"
                    )(MarkerContext(append("operation", gqlQuery.operation)))
                    cache.set(gqlQuery.toString, s)
                    appStart.CacheRegistrationCounter
                      .labelValues(gqlQuery.operation.getOrElse(""))
                      .inc()
                  }
                case Failure(exception) =>
                  logger.error(s"un caught exception running the query: ${exception.getMessage}",
                               exception
                  )
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
  )(implicit markerContext: MarkerContext): Future[Result] =
    QueryParser.parse(gqlQuery.query) match {

      // query parsed successfully, time to execute it!
      case Success(queryAst) =>
        var queryComplexity = -1.0
        Executor
          .execute(
            GQLSchema.schema,
            queryAst,
            dbTables,
            middleware = prometheusMetricsMiddleware :: Nil,
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
              logger.error(s"query analysis error ${graphQLError.toString}", error)
              BadRequest(error.resolveError)
            case error: ErrorWithResolver =>
              val graphQLError: GraphQLError =
                getErrorObject(gqlQuery, queryComplexity, error.getMessage())
              logger.error(s"error with resolver ${graphQLError.toString}", error)
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
