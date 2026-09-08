package models.entities

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult
import utils.OTLogging
import utils.db.DbJsonParser.fromPositionedResult

case class GeneEssentialityScreen(cellLineName: Option[String],
                                  depmapId: Option[String],
                                  diseaseCellLineId: Option[String],
                                  diseaseFromSource: Option[String],
                                  expression: Option[Double],
                                  geneEffect: Option[Double],
                                  mutation: Option[String]
)

case class DepMapEssentiality(screens: Seq[GeneEssentialityScreen],
                              tissueId: Option[String],
                              tissueName: Option[String]
)

case class TargetEssentiality(targetId: String,
                              isEssential: Option[Boolean],
                              depMapEssentiality: Seq[DepMapEssentiality]
)

object TargetEssentiality extends OTLogging {
  implicit val getTargetEssentialityResult: GetResult[TargetEssentiality] =
    GetResult(fromPositionedResult[TargetEssentiality])
  implicit val targetEssentialityImp: OFormat[TargetEssentiality] = Json.format[TargetEssentiality]
  implicit val depMapEssentialityImp: OFormat[DepMapEssentiality] = Json.format[DepMapEssentiality]
  implicit val geneEssentialityScreenImp: OFormat[GeneEssentialityScreen] =
    Json.format[GeneEssentialityScreen]
}
