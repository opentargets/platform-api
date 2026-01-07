package models.entities

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult
import utils.OTLogging

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

case class GeneEssentiality(isEssential: Option[Boolean],
                            depMapEssentiality: Seq[DepMapEssentiality]
)

case class TargetEssentiality(id: String, geneEssentiality: Seq[GeneEssentiality])

object TargetEssentiality extends OTLogging {
  implicit val getTargetEssentialityResult: GetResult[TargetEssentiality] =
    GetResult(r => Json.parse(r.<<[String]).as[TargetEssentiality])
  implicit val targetEssentialityImp: OFormat[TargetEssentiality] = Json.format[TargetEssentiality]
  implicit val geneEssentialityImp: OFormat[GeneEssentiality] = Json.format[GeneEssentiality]
  implicit val depMapEssentialityImp: OFormat[DepMapEssentiality] = Json.format[DepMapEssentiality]
  implicit val geneEssentialityScreenImp: OFormat[GeneEssentialityScreen] =
    Json.format[GeneEssentialityScreen]
}
