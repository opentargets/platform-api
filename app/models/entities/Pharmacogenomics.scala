package models.entities

import play.api.Logging
import play.api.libs.json._

case class Pharmacogenomics(
    drugId: Option[String],
    datasourceId: Option[String],
    datasourceVersion: Option[String],
    datatypeId: Option[String],
    drugFromSource: Option[String],
    evidenceLevel: Option[String],
    genotype: Option[String],
    genotypeAnnotationText: Option[String],
    genotypeId: Option[String],
    literature: Option[Seq[String]],
    pgxCategory: Option[String],
    phenotypeFromSourceId: Option[String],
    phenotypeText: Option[String],
    studyId: Option[String],
    targetFromSourceId: Option[String],
    variantFunctionalConsequenceId: Option[String],
    variantRsId: Option[String],
    isDirectTarget: Boolean
)


object Pharmacogenomics extends Logging {
  implicit val pharmacogenomicsW: OWrites[Pharmacogenomics] = Json.writes[Pharmacogenomics]
  implicit val pharmacogenomicsF: OFormat[Pharmacogenomics] = Json.format[Pharmacogenomics]
}
