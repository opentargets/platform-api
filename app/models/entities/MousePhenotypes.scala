package models.entities

import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class MousePhenotype(
                           biologicalModelAllelicComposition: String,
                           biologicalModelGeneticBackground: String,
                           biologicalModelId: Option[String],
                           literature: Seq[String],
                           modelPhenotypeClassId: String,
                           modelPhenotypeClassLabel: String,
                           modelPhenotypeId: String,
                           modelPhenotypeLabel: String,
                           targetFromSourceId: String,
                           targetInModel: String,
                           targetInModelEnsemblId: Option[String],
                           targetInModelMgiId: String
                         )


object MousePhenotypes extends Logging {

  implicit val mousePhenotypeW = Json.writes[MousePhenotype]

  implicit val mousePhenotypeR: Reads[MousePhenotype] = (
    (__ \ "biologicalModelAllelicComposition").read[String] and
      (__ \ "biologicalModelGeneticBackground").read[String] and
      (__ \ "biologicalModelId").readNullable[String] and
      (__ \ "literature").readWithDefault[Seq[String]](Seq.empty) and
      (__ \ "modelPhenotypeClassId").read[String] and
      (__ \ "modelPhenotypeClassLabel").read[String] and
      (__ \ "modelPhenotypeId").read[String] and
      (__ \ "modelPhenotypeLabel").read[String] and
      (__ \ "targetFromSourceId").read[String] and
      (__ \ "targetInModel").read[String] and
      (__ \ "targetInModelEnsemblId").readNullable[String] and
      (__ \ "targetInModelMgiId").read[String]
    )(MousePhenotype.apply _)

}
