package models.entities

import models.Helpers.logger
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonNaming.SnakeCase

case class OtarProject(otarCode: String, status: String, projectName: String, reference: String)

case class OtarProjects(efoId: String, rows: Seq[OtarProject])

object OtarProjects {
  implicit val otarProjectImpW = Json.writes[OtarProject]

  implicit val config = JsonConfiguration(SnakeCase)
  implicit val otarProjectImpR = Json.reads[OtarProject]

  implicit val otarProjectsImpW = Json.writes[OtarProjects]
  implicit val otarProjectsImpR: Reads[OtarProjects] =
    (
      (__ \ "efo_id").read[String] and
        (__ \ "projects").readWithDefault[Seq[OtarProject]](Seq.empty)
    )(OtarProjects.apply _)
}
