package models.entities

import models.Backend
import models.entities.CredibleSet.credibleSetImp
import models.gql.Fetchers.{
  credibleSetFetcher,
  diseasesFetcher,
  drugsFetcher,
  goFetcher,
  soTermsFetcher,
  targetsFetcher,
  variantFetcher
}
import models.gql.Objects
import models.gql.Objects.{diseaseImp, drugImp, geneOntologyTermImp, targetImp, variantIndexImp}
import play.api.Logging
import play.api.libs.json._
import sangria.schema.{
  Field,
  FloatType,
  ListType,
  LongType,
  ObjectType,
  OptionType,
  StringType,
  BooleanType
}

object Evidence extends Logging {

  import sangria.macros.derive._

  case class NameAndDescription(name: String, description: String)

  case class PathwayTerm(id: String, name: String)

  case class SequenceOntologyTerm(id: String, label: String)

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

  case class LabelledUri(url: String, niceName: String)

  case class BiomarkerGeneExpression(name: Option[String], id: Option[String])

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

  implicit val nameAndDescriptionJsonFormatImp: OFormat[NameAndDescription] =
    Json.format[NameAndDescription]

  implicit val pathwayTermJsonFormatImp: OFormat[PathwayTerm] = Json.format[PathwayTerm]

  implicit val sequenceOntologyTermJsonFormatImp: OFormat[SequenceOntologyTerm] =
    Json.format[SequenceOntologyTerm]

  implicit val evidenceTextMiningSentenceJsonFormatImp: OFormat[EvidenceTextMiningSentence] =
    Json.format[EvidenceTextMiningSentence]

  implicit val evidenceDiseaseCellLineJsonFormatImp: OFormat[EvidenceDiseaseCellLine] =
    Json.format[EvidenceDiseaseCellLine]

  implicit val evidenceVariationJsonFormatImp: OFormat[EvidenceVariation] =
    Json.format[EvidenceVariation]

  implicit val labelledElementJsonFormatImp: OFormat[LabelledElement] = Json.format[LabelledElement]

  implicit val labelledUriJsonFormatImp: OFormat[LabelledUri] = Json.format[LabelledUri]

  implicit val evidenceJsonFormatImp: OFormat[Evidence] = Json.format[Evidence]

  implicit val biomarkerGeneExpressionJsonFormatImp: OFormat[BiomarkerGeneExpression] =
    Json.format[BiomarkerGeneExpression]

  implicit val nameAndDescriptionImp: ObjectType[Backend, NameAndDescription] =
    deriveObjectType[Backend, NameAndDescription](
      ObjectTypeName("NameDescription")
    )

  implicit val pathwayTermImp: ObjectType[Backend, PathwayTerm] =
    deriveObjectType[Backend, PathwayTerm](
      ObjectTypeName("Pathway"),
      ObjectTypeDescription("Pathway entry")
    )

  implicit val sequenceOntologyTermImp: ObjectType[Backend, SequenceOntologyTerm] =
    deriveObjectType[Backend, SequenceOntologyTerm](
      ObjectTypeName("SequenceOntologyTerm"),
      ObjectTypeDescription("Sequence Ontology Term")
    )

  implicit val evidenceTextMiningSentenceImp: ObjectType[Backend, EvidenceTextMiningSentence] =
    deriveObjectType[Backend, EvidenceTextMiningSentence](
      ObjectTypeName("EvidenceTextMiningSentence")
    )

  implicit val evidenceDiseaseCellLineImp: ObjectType[Backend, EvidenceDiseaseCellLine] =
    deriveObjectType[Backend, EvidenceDiseaseCellLine](
      ObjectTypeName("DiseaseCellLine")
    )

  implicit val evidenceVariationImp: ObjectType[Backend, EvidenceVariation] =
    deriveObjectType[Backend, EvidenceVariation](
      ObjectTypeName("EvidenceVariation"),
      ObjectTypeDescription("Sequence Ontology Term"),
      ReplaceField(
        "functionalConsequenceId",
        Field(
          "functionalConsequence",
          OptionType(sequenceOntologyTermImp),
          description = None,
          resolve = js => {
            val soId = js.value.functionalConsequenceId.map(_.replace("_", ":"))
            soTermsFetcher.deferOpt(soId)
          }
        )
      )
    )

  implicit val labelledElementImp: ObjectType[Backend, LabelledElement] =
    deriveObjectType[Backend, LabelledElement](
      ObjectTypeName("LabelledElement")
    )

  implicit val labelledUriImp: ObjectType[Backend, LabelledUri] =
    deriveObjectType[Backend, LabelledUri](
      ObjectTypeName("LabelledUri")
    )

  implicit val biomarkerGeneExpressionImp: ObjectType[Backend, BiomarkerGeneExpression] =
    deriveObjectType[Backend, BiomarkerGeneExpression](
      ObjectTypeName("BiomarkerGeneExpression"),
      ReplaceField(
        "id",
        Field(
          "id",
          OptionType(geneOntologyTermImp),
          description = None,
          resolve = js => {
            val goId = js.value.id.map(_.replace('_', ':'))
            goFetcher.deferOpt(goId)
          }
        )
      )
    )

  case class BiomarkerVariant(id: Option[String],
                              name: Option[String],
                              functionalConsequenceId: Option[String]
  )

  implicit val biomarkerVariantJsonFormatImp: OFormat[BiomarkerVariant] =
    Json.format[BiomarkerVariant]

  implicit val biomarkerVariantImp: ObjectType[Backend, BiomarkerVariant] = deriveObjectType(
    ObjectTypeName("geneticVariation"),
    ReplaceField(
      "functionalConsequenceId",
      Field(
        "functionalConsequenceId",
        OptionType(sequenceOntologyTermImp),
        description = None,
        resolve = js => {
          val soId = js.value.functionalConsequenceId.map(_.replace("_", ":"))
          soTermsFetcher.deferOpt(soId)
        }
      )
    )
  )

  case class Biomarkers(geneExpression: Option[Seq[BiomarkerGeneExpression]],
                        geneticVariation: Option[Seq[BiomarkerVariant]]
  )

  implicit val biomarkersJsonFormatImp: OFormat[Biomarkers] = Json.format[Biomarkers]

  implicit val biomarkersImp: ObjectType[Backend, Biomarkers] = deriveObjectType(
    ObjectTypeName("biomarkers")
  )

  case class Assays(description: Option[String], isHit: Option[Boolean], shortName: Option[String])

  implicit val assaysJsonFormatImp: OFormat[Assays] = Json.format[Assays]

  implicit val assaysImp: ObjectType[Backend, Assays] = deriveObjectType(
    ObjectTypeName("assays")
  )

  val evidenceImp: ObjectType[Backend, Evidence] = deriveObjectType(
    ObjectTypeName("Evidence"),
    ObjectTypeDescription("Evidence for a Target-Disease pair"),
    DocumentField("id", "Evidence identifier"),
    DocumentField("score", "Evidence score"),
    DocumentField("variantRsId", "Variant dbSNP identifier"),
    DocumentField("oddsRatioConfidenceIntervalLower", "Confidence interval lower-bound"),
    DocumentField("studySampleSize", "Sample size"),
    DocumentField("literature", "list of pub med publications ids"),
    DocumentField("studyStopReasonCategories",
                  "Predicted reason(s) why the study has been stopped based on studyStopReason"
    ),
    DocumentField("ancestry", "Genetic origin of a population"),
    DocumentField("ancestryId", "Identifier of the ancestry in the HANCESTRO ontology"),
    DocumentField("statisticalMethod", "The statistical method used to calculate the association"),
    DocumentField("statisticalMethodOverview",
                  "Overview of the statistical method used to calculate the association"
    ),
    DocumentField(
      "studyCasesWithQualifyingVariants",
      "Number of cases in a case-control study that carry at least one allele of the qualifying variant"
    ),
    DocumentField("releaseVersion", "Release version"),
    DocumentField("releaseDate", "Release date"),
    DocumentField("warningMessage", "Warning message"),
    DocumentField("variantEffect", "Variant effect"),
    DocumentField("directionOnTrait", "Direction On Trait"),
    DocumentField("assessments", "Assessments"),
    DocumentField("primaryProjectHit", "Primary Project Hit"),
    DocumentField("primaryProjectId", "Primary Project Id"),
    ReplaceField(
      "targetId",
      Field(
        "target",
        targetImp,
        description = Some("Target evidence"),
        resolve = evidence => {
          val tId = evidence.value.targetId
          targetsFetcher.defer(tId)
        }
      )
    ),
    ReplaceField(
      "diseaseId",
      Field(
        "disease",
        diseaseImp,
        description = Some("Disease evidence"),
        resolve = evidence => {
          val tId = evidence.value.diseaseId
          diseasesFetcher.defer(tId)
        }
      )
    ),
    ReplaceField(
      "studyLocusId",
      Field(
        "credibleSet",
        OptionType(credibleSetImp),
        description = None,
        resolve = js => {
          val studyLocusId = js.value.studyLocusId
          credibleSetFetcher.deferOpt(studyLocusId)
        }
      )
    ),
    ReplaceField(
      "variantId",
      Field(
        "variant",
        OptionType(variantIndexImp),
        description = None,
        resolve = evidence => {
          val id = evidence.value.variantId
          logger.debug(s"Finding variant for id: $id")
          variantFetcher.deferOpt(id)
        }
      )
    ),
    ReplaceField(
      "drugId",
      Field(
        "drug",
        OptionType(drugImp),
        description = None,
        resolve = evidence => {
          val id = evidence.value.drugId
          logger.debug(s"Finding drug for id: $id")
          drugsFetcher.deferOpt(id)
        }
      )
    ),
    ReplaceField(
      "variantFunctionalConsequenceId",
      Field(
        "variantFunctionalConsequence",
        OptionType(sequenceOntologyTermImp),
        description = None,
        resolve = evidence => {
          val soId = evidence.value.variantFunctionalConsequenceId
            .map(id => id.replace("_", ":"))
          logger.error(s"Finding variant functional consequence: $soId")
          soTermsFetcher.deferOpt(soId)
        }
      )
    ),
    ReplaceField(
      "variantFunctionalConsequenceFromQtlId",
      Field(
        "variantFunctionalConsequenceFromQtlId",
        OptionType(sequenceOntologyTermImp),
        description = None,
        resolve = evidence => {
          val soId = evidence.value.variantFunctionalConsequenceFromQtlId
            .map(id => id.replace("_", ":"))
          soTermsFetcher.deferOpt(soId)
        }
      )
    ),
    ReplaceField(
      "pmcIds",
      Field(
        "pubMedCentralIds",
        OptionType(ListType(StringType)),
        description = Some("list of central pub med publications ids"),
        resolve = js => js.value.pmcIds
      )
    )
  )
}
