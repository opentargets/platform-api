package models.entities

import play.api.Logging
import play.api.libs.json._

case class NameAndDescription(name: String, description: String)

case class PathwayTerm(id: String, name: String)

case class EvidenceTextMiningSentence(
    dEnd: Long,
    tEnd: Long,
    dStart: Long,
    tStart: Long,
    section: String,
    text: String
)

case class EvidenceDiseaseCellLine(id: Option[String],
                                   name: Option[String],
                                   tissue: Option[String],
                                   tissueId: Option[String]
)

case class EvidenceVariation(
    functionalConsequenceId: Option[String],
    numberMutatedSamples: Option[Long],
    numberSamplesTested: Option[Long],
    numberSamplesWithMutationType: Option[Long]
)

case class LabelledElement(id: String, label: String)

case class LabelledUri(url: String, niceName: Option[String])

case class BiomarkerGeneExpression(name: Option[String], id: Option[String])

case class Biomarkers(geneExpression: Option[Seq[BiomarkerGeneExpression]],
                      geneticVariation: Option[Seq[BiomarkerVariant]]
)

case class BiomarkerVariant(id: Option[String],
                            name: Option[String],
                            functionalConsequenceId: Option[String]
)

case class Assays(description: Option[String], isHit: Option[Boolean], shortName: Option[String])

case class Evidence(
    id: String,
    score: Double,
    targetId: String,
    diseaseId: String,
    biomarkerName: Option[String],
    biomarkers: Option[Biomarkers],
    studyLocusId: Option[String],
    diseaseCellLines: Option[Seq[EvidenceDiseaseCellLine]],
    cohortPhenotypes: Option[Seq[String]],
    targetInModel: Option[String],
    reactionId: Option[String],
    reactionName: Option[String],
    projectId: Option[String],
    variantId: Option[String],
    variantRsId: Option[String],
    oddsRatioConfidenceIntervalLower: Option[Double],
    studySampleSize: Option[Long],
    variantAminoacidDescriptions: Option[Seq[String]],
    mutatedSamples: Option[Seq[EvidenceVariation]],
    drugId: Option[String],
    drugFromSource: Option[String],
    drugResponse: Option[String],
    cohortShortName: Option[String],
    diseaseModelAssociatedModelPhenotypes: Option[Seq[LabelledElement]],
    diseaseModelAssociatedHumanPhenotypes: Option[Seq[LabelledElement]],
    significantDriverMethods: Option[Seq[String]],
    pValueExponent: Option[Long],
    log2FoldChangePercentileRank: Option[Long],
    biologicalModelAllelicComposition: Option[String],
    confidence: Option[String],
    clinicalPhase: Option[Double],
    resourceScore: Option[Double],
    variantFunctionalConsequenceId: Option[String],
    variantFunctionalConsequenceFromQtlId: Option[String],
    biologicalModelGeneticBackground: Option[String],
    urls: Option[Seq[LabelledUri]],
    literature: Option[Seq[String]],
    pmcIds: Option[Seq[String]],
    studyCases: Option[Long],
    studyOverview: Option[String],
    allelicRequirements: Option[Seq[String]],
    datasourceId: String,
    datatypeId: String,
    oddsRatioConfidenceIntervalUpper: Option[Double],
    clinicalStatus: Option[String],
    log2FoldChangeValue: Option[Double],
    oddsRatio: Option[Double],
    cohortDescription: Option[String],
    publicationYear: Option[Long],
    diseaseFromSource: Option[String],
    diseaseFromSourceId: Option[String],
    targetFromSourceId: Option[String],
    targetModulation: Option[String],
    textMiningSentences: Option[Seq[EvidenceTextMiningSentence]],
    studyId: Option[String],
    clinicalSignificances: Option[Seq[String]],
    cohortId: Option[String],
    pValueMantissa: Option[Double],
    pathways: Option[Seq[PathwayTerm]],
    publicationFirstAuthor: Option[String],
    alleleOrigins: Option[Seq[String]],
    biologicalModelId: Option[String],
    biosamplesFromSource: Option[Seq[String]],
    diseaseFromSourceMappedId: Option[String],
    beta: Option[Double],
    betaConfidenceIntervalLower: Option[Double],
    betaConfidenceIntervalUpper: Option[Double],
    studyStartDate: Option[String],
    studyStopReason: Option[String],
    studyStopReasonCategories: Option[Seq[String]],
    targetFromSource: Option[String],
    cellLineBackground: Option[String],
    contrast: Option[String],
    crisprScreenLibrary: Option[String],
    cellType: Option[String],
    statisticalTestTail: Option[String],
    interactingTargetFromSourceId: Option[String],
    phenotypicConsequenceLogFoldChange: Option[Double],
    phenotypicConsequenceFDR: Option[Double],
    phenotypicConsequencePValue: Option[Double],
    geneticInteractionScore: Option[Double],
    geneticInteractionPValue: Option[Double],
    geneticInteractionFDR: Option[Double],
    biomarkerList: Option[Seq[NameAndDescription]],
    projectDescription: Option[String],
    geneInteractionType: Option[String],
    targetRole: Option[String],
    interactingTargetRole: Option[String],
    ancestry: Option[String],
    ancestryId: Option[String],
    statisticalMethod: Option[String],
    statisticalMethodOverview: Option[String],
    studyCasesWithQualifyingVariants: Option[Long],
    releaseVersion: Option[String],
    releaseDate: Option[String],
    warningMessage: Option[String],
    variantEffect: Option[String],
    directionOnTrait: Option[String],
    assessments: Option[Seq[String]],
    primaryProjectHit: Option[Boolean],
    primaryProjectId: Option[String],
    assays: Option[Seq[Assays]]
)

object Evidence extends Logging {

  implicit val nameAndDescriptionJsonFormatImp: OFormat[NameAndDescription] =
    Json.format[NameAndDescription]

  implicit val pathwayTermJsonFormatImp: OFormat[PathwayTerm] = Json.format[PathwayTerm]

  implicit val evidenceTextMiningSentenceJsonFormatImp: OFormat[EvidenceTextMiningSentence] =
    Json.format[EvidenceTextMiningSentence]

  implicit val evidenceDiseaseCellLineJsonFormatImp: OFormat[EvidenceDiseaseCellLine] =
    Json.format[EvidenceDiseaseCellLine]

  implicit val evidenceVariationJsonFormatImp: OFormat[EvidenceVariation] =
    Json.format[EvidenceVariation]

  implicit val labelledElementJsonFormatImp: OFormat[LabelledElement] = Json.format[LabelledElement]

  implicit val labelledUriJsonFormatImp: OFormat[LabelledUri] = Json.format[LabelledUri]

  implicit val biomarkerVariantJsonFormatImp: OFormat[BiomarkerVariant] =
    Json.format[BiomarkerVariant]

  implicit val assaysJsonFormatImp: OFormat[Assays] = Json.format[Assays]

  implicit val biomarkersJsonFormatImp: OFormat[Biomarkers] = Json.format[Biomarkers]

  implicit val evidenceJsonFormatImp: OFormat[Evidence] = Json.format[Evidence]

  implicit val biomarkerGeneExpressionJsonFormatImp: OFormat[BiomarkerGeneExpression] =
    Json.format[BiomarkerGeneExpression]

}
