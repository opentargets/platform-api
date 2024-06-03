package models.entities

import play.api.Logging
import play.api.libs.json._

case class PharmacogenomicsDrug(
    drugId: Option[String],
    drugFromSource: Option[String]   
)

case class Pharmacogenomics(
    datasourceId: Option[String],
    datatypeId: Option[String],
    drugs: Option[Seq[PharmacogenomicsDrug]],
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
  implicit val drugW: OWrites[PharmacogenomicsDrug] = Json.writes[PharmacogenomicsDrug]
  implicit val drugF: OFormat[PharmacogenomicsDrug] = Json.format[PharmacogenomicsDrug]
  implicit val pharmacogenomicsW: OWrites[Pharmacogenomics] = Json.writes[Pharmacogenomics]
  implicit val pharmacogenomicsF: OFormat[Pharmacogenomics] = Json.format[Pharmacogenomics]
}
