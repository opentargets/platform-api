package models.entities

import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class BiologicalModels(
                             allelicComposition: String,
                             geneticBackground: String,
                             id: Option[String],
                             literature: Option[Seq[String]]
                           )

case class ModelPhenotypeClasses(
                                  id: String,
                                  label: String
                                )

case class MousePhenotype(
                           biologicalModels: Seq[BiologicalModels],
                           modelPhenotypeClasses: Seq[ModelPhenotypeClasses],
                           modelPhenotypeId: String,
                           modelPhenotypeLabel: String,
                           targetFromSourceId: String,
                           targetInModel: String,
                           targetInModelEnsemblId: Option[String],
                           targetInModelMgiId: String
                         )

object MousePhenotypes extends Logging {

  implicit val biologicalModelsF: OFormat[BiologicalModels] = Json.format[BiologicalModels]
  implicit val modelPhenotypeClassesF: OFormat[ModelPhenotypeClasses] =
    Json.format[ModelPhenotypeClasses]
  implicit val mousePhenotypeW: OWrites[MousePhenotype] = Json.writes[MousePhenotype]

  implicit val mousePhenotypeR: Reads[MousePhenotype] = (
    (__ \ "biologicalModels").read[Seq[BiologicalModels]] and
      (__ \ "modelPhenotypeClasses").read[Seq[ModelPhenotypeClasses]] and
      (__ \ "modelPhenotypeId").read[String] and
      (__ \ "modelPhenotypeLabel").read[String] and
      (__ \ "targetFromSourceId").read[String] and
      (__ \ "targetInModel").read[String] and
      (__ \ "targetInModelEnsemblId").readNullable[String] and
      (__ \ "targetInModelMgiId").read[String]
    ) (MousePhenotype.apply _)

}
