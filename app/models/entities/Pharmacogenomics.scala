package models.entities

import play.api.Logging
import play.api.libs.json._

case class Drug(
    drugId: Option[String],
    drugFromSource: Option[String]   
)

case class Pharmacogenomics(
    datasourceId: Option[String],
    datatypeId: Option[String],
    drugs: Option[Seq[Drug]],
    evidenceLevel: Option[String],
    genotype: Option[String],
    genotypeAnnotationText: Option[String],
    genotypeId: Option[String],
    haplotypeFromSourceId: Option[String],
    haplotypeId: Option[String],
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
  implicit val drugW: OWrites[Drug] = Json.writes[Drug]
  implicit val drugF: OFormat[Drug] = Json.format[Drug]
  implicit val pharmacogenomicsW: OWrites[Pharmacogenomics] = Json.writes[Pharmacogenomics]
  implicit val pharmacogenomicsF: OFormat[Pharmacogenomics] = Json.format[Pharmacogenomics]
}
