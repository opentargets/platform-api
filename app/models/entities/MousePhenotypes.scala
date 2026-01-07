package models.entities

import play.api.Logging
import play.api.libs.json._
import slick.jdbc.GetResult
import models.gql.TypeWithId
import utils.OTLogging

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

case class MousePhenotypes(
    count: Long,
    rows: IndexedSeq[MousePhenotype],
    id: String = ""
) extends TypeWithId

object MousePhenotypes extends OTLogging {
  implicit val getFromDB: GetResult[MousePhenotypes] =
    GetResult(r => Json.parse(r.<<[String]).as[MousePhenotypes])
  implicit val biologicalModelsF: OFormat[BiologicalModels] = Json.format[BiologicalModels]
  implicit val modelPhenotypeClassesF: OFormat[ModelPhenotypeClasses] =
    Json.format[ModelPhenotypeClasses]
  implicit val mousePhenotypeF: OFormat[MousePhenotype] = Json.format[MousePhenotype]
  implicit val mousePhenotypesF: OFormat[MousePhenotypes] = Json.format[MousePhenotypes]
}
