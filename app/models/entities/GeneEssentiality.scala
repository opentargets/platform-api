package models.entities

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
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

case class TargetEssentiality(id: Option[String], geneEssentiality: Seq[GeneEssentiality])

object TargetEssentiality extends OTLogging {

  implicit val geneEssentialityScreenImpW: OWrites[GeneEssentialityScreen] =
    Json.writes[models.entities.GeneEssentialityScreen]
  implicit val geneEssentialityScreenImpR: Reads[models.entities.GeneEssentialityScreen] =
    ((__ \ "cellLineName").readNullable[String] and
      (__ \ "depmapId").readNullable[String] and
      (__ \ "diseaseCellLineId").readNullable[String] and
      (__ \ "diseaseFromSource").readNullable[String] and
      (__ \ "expression").readNullable[Double] and
      (__ \ "geneEffect").readNullable[Double] and
      (__ \ "mutation").readNullable[String])(GeneEssentialityScreen.apply)

  implicit val depMapEssentialityImpW: OWrites[DepMapEssentiality] =
    Json.writes[models.entities.DepMapEssentiality]
  implicit val depMapEssentialityImpR: Reads[models.entities.DepMapEssentiality] =
    ((__ \ "screens").readWithDefault[Seq[GeneEssentialityScreen]](Seq.empty) and
      (__ \ "tissueId").readNullable[String] and
      (__ \ "tissueName").readNullable[String])(DepMapEssentiality.apply)

  implicit val geneEssentialityImpW: OWrites[GeneEssentiality] = Json.writes[GeneEssentiality]
  implicit val geneEssentialityImpR: Reads[GeneEssentiality] =
    ((__ \ "isEssential").readNullable[Boolean] and
      (__ \ "depMapEssentiality").read[Seq[DepMapEssentiality]])(GeneEssentiality.apply)

  implicit val targetEssentialityImpW: OWrites[TargetEssentiality] = Json.writes[TargetEssentiality]
  implicit val targetEssentialityImpR: Reads[TargetEssentiality] =
    ((__ \ "id").readNullable[String] and
      (__ \ "geneEssentiality").read[Seq[GeneEssentiality]])(TargetEssentiality.apply)

}
