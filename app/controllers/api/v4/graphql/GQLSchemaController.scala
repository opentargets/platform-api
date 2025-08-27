package controllers.api.v4.graphql

import javax.inject.{Inject, Singleton}
import models.GQLSchema
import play.api.mvc.{AbstractController, ControllerComponents}
import sangria.renderer.SchemaRenderer

import scala.concurrent.ExecutionContext
import play.api.mvc
import play.api.mvc.AnyContent

@Singleton
class GQLSchemaController @Inject() (implicit ec: ExecutionContext, cc: ControllerComponents)
    extends AbstractController(cc) {

  def renderSchema: mvc.Action[AnyContent] = Action {
    Ok(SchemaRenderer.renderSchema(GQLSchema.schema))
  }

  def renderPlayground: mvc.Action[AnyContent] = Action {
    Ok(views.html.playground())
  }
}
