package controllers.api.v4.rest

import javax.inject._
import models.Entities.JSONImplicits._
import models.Entities.TargetsBody
import models.entities.APIErrorMessage.JSONImplicits._
import models.entities.Target.JSONImplicits._
import models.entities._
import models.entities.Harmonic.Association.JSONImplicits._
import models.{Backend, GQLSchema}
import play.api.libs.json._
import play.api.mvc._
import sangria.execution.deferred.FetcherContext

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AssociationController @Inject()(implicit ec: ExecutionContext, backend: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {

  def byTargetId(id:String, expansionId: Option[String], index: Int, size: Int) = Action.async { req =>
    for {
      associations <- backend.getAssociationsTargetFixed(id, expansionId, Pagination(index, size))
    } yield associations match {
      case _ if associations.isEmpty => NotFound(Json.toJson(APIErrorMessage(NOT_FOUND, s"No associations for the target $id")))
      case v if associations.nonEmpty => Ok(Json.toJson(v))
    }
  }

  def byDiseaseId(id:String, expansionId: Option[String], index: Int, size: Int) = Action.async { req =>
    for {
      associations <- backend.getAssociationsDiseaseFixed(id, expansionId, Pagination(index, size))
    } yield associations match {
      case _ if associations.isEmpty => NotFound(Json.toJson(APIErrorMessage(NOT_FOUND, s"No associations for the disease $id")))
      case v if associations.nonEmpty => Ok(Json.toJson(v))
    }
  }
}
