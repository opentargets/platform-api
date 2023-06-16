package models.entities

import play.api.Logging
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.Seq

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

object GeneEssentiality extends Logging {

  implicit val geneEssentialityScreenImpW: OWrites[GeneEssentialityScreen] =
    Json.writes[models.entities.GeneEssentialityScreen]
  implicit val geneEssentialityScreenImpR: Reads[models.entities.GeneEssentialityScreen] =
    ((__ \ "cellLineName").readNullable[String] and
      (__ \ "depmapId").readNullable[String] and
      (__ \ "diseaseCellLineId").readNullable[String] and
      (__ \ "diseaseFromSource").readNullable[String] and
      (__ \ "expression").readNullable[Double] and
      (__ \ "geneEffect").readNullable[Double] and
      (__ \ "mutation").readNullable[String])(GeneEssentialityScreen.apply _)

  implicit val depMapEssentialityImpW: OWrites[DepMapEssentiality] =
    Json.writes[models.entities.DepMapEssentiality]
  implicit val depMapEssentialityImpR: Reads[models.entities.DepMapEssentiality] =
    ((__ \ "screens").readWithDefault[Seq[GeneEssentialityScreen]](Seq.empty) and
      (__ \ "tissueId").readNullable[String] and
      (__ \ "tissueName").readNullable[String])(DepMapEssentiality.apply _)

  implicit val geneEssentialityImpW: OWrites[GeneEssentiality] = Json.writes[GeneEssentiality]
  implicit val geneEssentialityImpR: Reads[GeneEssentiality] =
    ((__ \ "isEssential").readNullable[Boolean] and
      (__ \ "depMapEssentiality").read[Seq[DepMapEssentiality]])(GeneEssentiality.apply _)

}
