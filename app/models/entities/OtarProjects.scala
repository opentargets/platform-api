package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonNaming.SnakeCase
import slick.jdbc.GetResult

case class OtarProject(otarCode: String,
                       status: Option[String],
                       projectName: Option[String],
                       reference: String,
                       integratesInPPP: Option[Boolean]
)

case class OtarProjects(efoId: String, rows: Seq[OtarProject])

object OtarProjects {
  implicit val getOtarProjectsResult: GetResult[OtarProjects] =
    GetResult(r => Json.parse(r.<<[String]).as[OtarProjects])
  implicit val config: JsonConfiguration.Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
  implicit val otarProjectImpW: OWrites[OtarProject] = Json.writes[OtarProject]

  implicit val otarProjectImpR: Reads[OtarProject] =
    ((JsPath \ "otar_code").read[String] and
      (JsPath \ "status").readNullable[String] and
      (JsPath \ "project_name").readNullable[String] and
      (JsPath \ "reference").read[String] and
      (JsPath \ "integrates_data_PPP").readNullable[Boolean])(OtarProject.apply)

  implicit val otarProjectsImpW: OWrites[OtarProjects] = Json.writes[OtarProjects]
  implicit val otarProjectsImpR: Reads[OtarProjects] =
    (
      (__ \ "efo_id").read[String] and
        (__ \ "projects").readWithDefault[Seq[OtarProject]](Seq.empty)
    )(OtarProjects.apply)
}
