package models.entities

import play.api.libs.json.*
import utils.OTLogging
import slick.jdbc.GetResult

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

case class PharmacogenomicsByDrug(drugId: String, pharmacogenomics: Seq[Pharmacogenomics])
case class PharmacogenomicsByVariant(variantId: String, pharmacogenomics: Seq[Pharmacogenomics])
case class PharmacogenomicsByTarget(targetFromSourceId: String,
                                    pharmacogenomics: Seq[Pharmacogenomics]
)

object Pharmacogenomics extends OTLogging {
  implicit val getPharmacogenomicsByDrugResult: GetResult[PharmacogenomicsByDrug] =
    GetResult(r => Json.parse(r.<<[String]).as[PharmacogenomicsByDrug])
  implicit val getPharmacogenomicsByVariantResult: GetResult[PharmacogenomicsByVariant] =
    GetResult(r => Json.parse(r.<<[String]).as[PharmacogenomicsByVariant])
  implicit val getPharmacogenomicsByTargetResult: GetResult[PharmacogenomicsByTarget] =
    GetResult(r => Json.parse(r.<<[String]).as[PharmacogenomicsByTarget])
  implicit val drugF: OFormat[DrugWithIdentifiers] = Json.format[DrugWithIdentifiers]
  implicit val variantAnnotation: OFormat[VariantAnnotation] = Json.format[VariantAnnotation]
  implicit val pharmacogenomicsF: OFormat[Pharmacogenomics] = Json.format[Pharmacogenomics]
  implicit val pharmacogenomicsByDrugF: OFormat[PharmacogenomicsByDrug] =
    Json.format[PharmacogenomicsByDrug]
  implicit val pharmacogenomicsByVariantF: OFormat[PharmacogenomicsByVariant] =
    Json.format[PharmacogenomicsByVariant]
  implicit val pharmacogenomicsByTargetF: OFormat[PharmacogenomicsByTarget] =
    Json.format[PharmacogenomicsByTarget]
}
