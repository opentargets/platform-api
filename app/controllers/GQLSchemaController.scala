package controllers

import javax.inject.{Inject, Singleton}
import models.GQLSchema
import play.api.mvc.{AbstractController, ControllerComponents}
import sangria.renderer.SchemaRenderer

import scala.concurrent.ExecutionContext

@Singleton
class GQLSchemaController @Inject()(implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AbstractController(cc) {

  def renderSchema = Action {
    Ok(SchemaRenderer.renderSchema(GQLSchema.schema))
  }

  def renderClient = Action {
    Ok(views.html.graphiql(None))
  }
}
