package models.entities

import play.api.Logging
import play.api.libs.json._

case class DrugWithIdentifiers(
    drugId: Option[String],
    drugFromSource: Option[String]
)

case class VariantAnnotation(baseAlleleOrGenotype: Option[String],
                             comparisonAlleleOrGenotype: Option[String],
                             directionality: Option[String],
                             effect: Option[String],
                             effectDescription: Option[String],
                             effectType: Option[String],
                             entity: Option[String],
                             literature: Option[String]
)

case class Pharmacogenomics(
    datasourceId: Option[String],
    datatypeId: Option[String],
    drugs: Seq[DrugWithIdentifiers],
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
    variantAnnotation: Option[Seq[VariantAnnotation]],
    studyId: Option[String],
    targetFromSourceId: Option[String],
    variantFunctionalConsequenceId: Option[String],
    variantRsId: Option[String],
    variantId: Option[String],
    isDirectTarget: Boolean
)

object Pharmacogenomics extends Logging {
  implicit val drugW: OWrites[DrugWithIdentifiers] = Json.writes[DrugWithIdentifiers]
  implicit val drugF: OFormat[DrugWithIdentifiers] = Json.format[DrugWithIdentifiers]
  implicit val variantAnnotation: OFormat[VariantAnnotation] = Json.format[VariantAnnotation]
  implicit val pharmacogenomicsW: OWrites[Pharmacogenomics] = Json.writes[Pharmacogenomics]
  implicit val pharmacogenomicsF: OFormat[Pharmacogenomics] = Json.format[Pharmacogenomics]
}
