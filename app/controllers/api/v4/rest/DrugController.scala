package controllers.api.v4.rest

import javax.inject._
import models.Entities.JSONImplicits._
import models.Entities._
import models.entities._
import models.entities.APIErrorMessage.JSONImplicits._
import models.entities.Drug.JSONImplicits._
import models.entities.Violations.InputParameterCheckError
import models.{Backend, GQLSchema}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import sangria.ast.OperationType.Query
import sangria.ast._
import sangria.ast
import sangria.execution.{ErrorWithResolver, ExceptionHandler, Executor, HandledException, MaxQueryDepthReachedError, QueryAnalysisError, QueryReducer}
import sangria.execution.deferred.FetcherContext
import sangria.marshalling.playJson._
import sangria.macros._
import sangria.marshalling.InputUnmarshaller
import sangria.parser.{QueryParser, SyntaxError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class DrugController @Inject()(implicit ec: ExecutionContext,
                               backend: Backend,
                               cc: ControllerComponents)
  extends AbstractController(cc) {
  val logger = Logger(this.getClass)

  val drugsGQLQ =
    gql"""
        query drugsQuery($$ids: [String!]!) {
          drugs(chemblIds: $$ids) {
            id
            name
            synonyms
            tradeNames
            yearOfFirstApproval
            drugType
            maximumClinicalTrialPhase
            hasBeenWithdrawn
            withdrawnNotice {
              classes
              countries
              reasons
              year
            }
            internalCompound
            mechanismsOfAction {
              rows {
                mechanismOfAction
                targetName
                references {
                  ids
                  source
                  urls
                }
                targets {
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
                  }
                }
              }
            }
            linkedDiseases {
              count
              rows
            }
            linkedTargets {
              count
              rows {
                id
                hgncId
                approvedSymbol
                approvedName
              }
            }
          }
        }
      """

  val drugGQLQ =
    gql"""
        query drugQuery($$id: String!) {
          drug(chemblId: $$id) {
            id
            name
            synonyms
            tradeNames
            yearOfFirstApproval
            drugType
            maximumClinicalTrialPhase
            hasBeenWithdrawn
            withdrawnNotice {
              classes
              countries
              reasons
              year
            }
            internalCompound
            mechanismsOfAction {
              rows {
                mechanismOfAction
                targetName
                references {
                  ids
                  source
                  urls
                }
                targets {
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
                  }
                }
              }
            }
            linkedDiseases {
              count
              rows
            }
            linkedTargets {
              count
              rows {
                id
                hgncId
                approvedSymbol
                approvedName
              }
            }
          }
        }
      """

  private def queryDrugs(drugIds: Seq[String]) = {
    logger.debug(s"parsed document: ${drugsGQLQ.renderPretty}")

    Executor.execute(GQLSchema.schema, drugsGQLQ, backend,
      variables = InputUnmarshaller.mapVars(Map("ids" -> drugIds)),
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

  private def queryDrug(drugId: String) = {
    logger.debug(s"parsed document: ${drugGQLQ.renderPretty}")

    Executor.execute(GQLSchema.schema, drugGQLQ, backend,
      variables = InputUnmarshaller.mapVars(Map("id" -> drugId)),
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
//      drugs <- GQLSchema.drugsFetcher.fetch(ctxD, Seq(id))
//    } yield drugs.headOption match {
//      case None => NotFound(Json.toJson(APIErrorMessage(NOT_FOUND, s"$id not found")))
//      case Some(t) => Ok(Json.toJson(t))
//    }
    queryDrug(id)
  }

  def byIds(ids: Seq[String]) = Action.async { req =>
    (req.method, ids) match {
      case ("POST", _ :: _) | ("GET", _ :: _) =>
        queryDrugs(ids)

      case ("POST", Nil) =>
        req.body.asJson.map(_.as[TargetsBody]) match {
          case Some(body) =>
            queryDrugs(body.ids)

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
