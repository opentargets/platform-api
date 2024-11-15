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
  BooleanType,
  fields
}

object Evidence extends Logging {

  import sangria.macros.derive._

  case class NameAndDescription(name: String, description: String)

  implicit val nameAndDescriptionJsonFormatImp: OFormat[NameAndDescription] =
    Json.format[NameAndDescription]

  val nameAndDescriptionImp: ObjectType[Backend, NameAndDescription] =
    deriveObjectType[Backend, NameAndDescription](
      ObjectTypeName("NameDescription")
    )
  val pathwayTermImp: ObjectType[Backend, JsValue] = ObjectType(
    "Pathway",
    "Pathway entry",
    fields[Backend, JsValue](
      Field(
        "id",
        StringType,
        description = Some("Pathway ID"),
        resolve = js => (js.value \ "id").as[String]
      ),
      Field(
        "name",
        StringType,
        description = Some("Pathway Name"),
        resolve = js => (js.value \ "name").as[String]
      )
    )
  )

  val sequenceOntologyTermImp: ObjectType[Backend, JsValue] = ObjectType(
    "SequenceOntologyTerm",
    "Sequence Ontology Term",
    fields[Backend, JsValue](
      Field(
        "id",
        StringType,
        description = Some("Sequence Ontology ID"),
        resolve = js => (js.value \ "id").as[String]
      ),
      Field(
        "label",
        StringType,
        description = Some("Sequence Ontology Label"),
        resolve = js => (js.value \ "label").as[String]
      )
    )
  )

  val evidenceTextMiningSentenceImp: ObjectType[Backend, JsValue] = ObjectType(
    "EvidenceTextMiningSentence",
    fields[Backend, JsValue](
      Field("dEnd", LongType, description = None, resolve = js => (js.value \ "dEnd").as[Long]),
      Field("tEnd", LongType, description = None, resolve = js => (js.value \ "tEnd").as[Long]),
      Field("dStart", LongType, description = None, resolve = js => (js.value \ "dStart").as[Long]),
      Field("tStart", LongType, description = None, resolve = js => (js.value \ "tStart").as[Long]),
      Field(
        "section",
        StringType,
        description = None,
        resolve = js => (js.value \ "section").as[String]
      ),
      Field("text", StringType, description = None, resolve = js => (js.value \ "text").as[String])
    )
  )
  val evidenceDiseaseCellLineImp: ObjectType[Backend, JsValue] = ObjectType(
    "DiseaseCellLine",
    fields[Backend, JsValue](
      Field("id",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "id").asOpt[String]
      ),
      Field("name",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "name").asOpt[String]
      ),
      Field(
        "tissue",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "tissue").asOpt[String]
      ),
      Field(
        "tissueId",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "tissueId").asOpt[String]
      )
    )
  )

  val evidenceVariationImp: ObjectType[Backend, JsValue] = ObjectType(
    "EvidenceVariation",
    "Sequence Ontology Term",
    fields[Backend, JsValue](
      Field(
        "functionalConsequence",
        OptionType(sequenceOntologyTermImp),
        description = None,
        resolve = js => {
          val soId = ((js.value \ "functionalConsequenceId").asOpt[String]).map(_.replace("_", ":"))
          soTermsFetcher.deferOpt(soId)
        }
      ),
      Field(
        "numberMutatedSamples",
        OptionType(LongType),
        description = None,
        resolve = js => (js.value \ "numberMutatedSamples").asOpt[Long]
      ),
      Field(
        "numberSamplesTested",
        OptionType(LongType),
        description = None,
        resolve = js => (js.value \ "numberSamplesTested").asOpt[Long]
      ),
      Field(
        "numberSamplesWithMutationType",
        OptionType(LongType),
        description = None,
        resolve = js => (js.value \ "numberSamplesWithMutationType").asOpt[Long]
      )
    )
  )

  val labelledElementImp: ObjectType[Backend, JsValue] = ObjectType(
    "LabelledElement",
    fields[Backend, JsValue](
      Field("id", StringType, description = None, resolve = js => (js.value \ "id").as[String]),
      Field(
        "label",
        StringType,
        description = None,
        resolve = js => (js.value \ "label").as[String]
      )
    )
  )

  val labelledUriImp: ObjectType[Backend, JsValue] = ObjectType(
    "LabelledUri",
    fields[Backend, JsValue](
      Field("url", StringType, description = None, resolve = js => (js.value \ "url").as[String]),
      Field(
        "niceName",
        StringType,
        description = None,
        resolve = js => (js.value \ "niceName").as[String]
      )
    )
  )

  val biomarkerGeneExpressionImp: ObjectType[Backend, JsValue] = ObjectType(
    "geneExpression",
    fields[Backend, JsValue](
      Field(
        "name",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "name").asOpt[String]
      ),
      Field(
        "id",
        OptionType(geneOntologyTermImp),
        description = None,
        resolve = js => {
          val goId = (js.value \ "id").asOpt[String].map(_.replace('_', ':'))
          goFetcher.deferOpt(goId)
        }
      )
    )
  )
  val biomarkerVariantImp: ObjectType[Backend, JsValue] = ObjectType(
    "geneticVariation",
    fields[Backend, JsValue](
      Field(
        "id",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "id").asOpt[String]
      ),
      Field(
        "name",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "name").asOpt[String]
      ),
      Field(
        "functionalConsequenceId",
        OptionType(sequenceOntologyTermImp),
        description = None,
        resolve = js => {
          val soId = (js.value \ "functionalConsequenceId").asOpt[String].map(_.replace("_", ":"))
          soTermsFetcher.deferOpt(soId)
        }
      )
    )
  )
  val biomarkersImp: ObjectType[Backend, JsValue] = ObjectType(
    "biomarkers",
    fields[Backend, JsValue](
      Field(
        "geneExpression",
        OptionType(ListType(biomarkerGeneExpressionImp)),
        description = None,
        resolve = js => (js.value \ "geneExpression").asOpt[Seq[JsValue]]
      ),
      Field(
        "geneticVariation",
        OptionType(ListType(biomarkerVariantImp)),
        description = None,
        resolve = js => (js.value \ "geneticVariation").asOpt[Seq[JsValue]]
      )
    )
  )
  val assaysImp: ObjectType[Backend, JsValue] = ObjectType(
    "assays",
    fields[Backend, JsValue](
      Field(
        "description",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "description").asOpt[String]
      ),
      Field(
        "isHit",
        OptionType(BooleanType),
        description = None,
        resolve = js => (js.value \ "isHit").asOpt[Boolean]
      ),
      Field(
        "shortName",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "shortName").asOpt[String]
      )
    )
  )

  val evidenceImp: ObjectType[Backend, JsValue] = ObjectType(
    "Evidence",
    "Evidence for a Target-Disease pair",
    fields[Backend, JsValue](
      Field(
        "id",
        StringType,
        description = Some("Evidence identifier"),
        resolve = js => (js.value \ "id").as[String]
      ),
      Field(
        "score",
        FloatType,
        description = Some("Evidence score"),
        resolve = js => (js.value \ "score").as[Double]
      ),
      Field(
        "target",
        targetImp,
        description = Some("Target evidence"),
        resolve = js => {
          val tId = (js.value \ "targetId").as[String]
          targetsFetcher.defer(tId)
        }
      ),
      Field(
        "disease",
        diseaseImp,
        description = Some("Disease evidence"),
        resolve = js => {
          val dId = (js.value \ "diseaseId").as[String]
          diseasesFetcher.defer(dId)
        }
      ),
      Field(
        "biomarkerName",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "biomarkerName").asOpt[String]
      ),
      Field(
        "biomarkers",
        OptionType(biomarkersImp),
        description = None,
        resolve = js => (js.value \ "biomarkers").asOpt[JsValue]
      ),
      Field(
        "credibleSet",
        OptionType(credibleSetImp),
        description = None,
        resolve = js => {
          val studyLocusId = (js.value \ "studyLocusId").asOpt[String]
          credibleSetFetcher.deferOpt(studyLocusId)
        }
      ),
      Field(
        "diseaseCellLines",
        OptionType(ListType(evidenceDiseaseCellLineImp)),
        description = None,
        resolve = js => (js.value \ "diseaseCellLines").asOpt[Seq[JsValue]]
      ),
      Field(
        "cohortPhenotypes",
        OptionType(ListType(StringType)),
        description = None,
        resolve = js => (js.value \ "cohortPhenotypes").asOpt[Seq[String]]
      ),
      Field(
        "targetInModel",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "targetInModel").asOpt[String]
      ),
      Field(
        "reactionId",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "reactionId").asOpt[String]
      ),
      Field(
        "reactionName",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "reactionName").asOpt[String]
      ),
      Field(
        "projectId",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "projectId").asOpt[String]
      ),
      Field(
        "variant",
        OptionType(variantIndexImp),
        description = None,
        resolve = js => {
          val id = (js.value \ "variantId").asOpt[String]
          logger.debug(s"Finding variant for id: $id")
          variantFetcher.deferOpt(id)
        }
      ),
      Field(
        "variantRsId",
        OptionType(StringType),
        description = Some("Variant dbSNP identifier"),
        resolve = js => (js.value \ "variantRsId").asOpt[String]
      ),
      Field(
        "oddsRatioConfidenceIntervalLower",
        OptionType(FloatType),
        description = Some("Confidence interval lower-bound  "),
        resolve = js => (js.value \ "oddsRatioConfidenceIntervalLower").asOpt[Double]
      ),
      Field(
        "studySampleSize",
        OptionType(LongType),
        description = Some("Sample size"),
        resolve = js => (js.value \ "studySampleSize").asOpt[Long]
      ),
      Field(
        "variantAminoacidDescriptions",
        OptionType(ListType(StringType)),
        description = None,
        resolve = js => (js.value \ "variantAminoacidDescriptions").asOpt[Seq[String]]
      ),
      Field(
        "mutatedSamples",
        OptionType(ListType(evidenceVariationImp)),
        description = None,
        resolve = js => (js.value \ "mutatedSamples").asOpt[Seq[JsValue]]
      ),
      Field(
        "drug",
        OptionType(drugImp),
        description = None,
        resolve = js => {
          val drugId = (js.value \ "drugId").asOpt[String]
          drugsFetcher.deferOpt(drugId)
        }
      ),
      Field(
        "drugFromSource",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "drugFromSource").asOpt[String]
      ),
      Field(
        "drugResponse",
        OptionType(Objects.diseaseImp),
        description = None,
        resolve = js => {
          val efoId = (js.value \ "drugResponse").asOpt[String]
          diseasesFetcher.deferOpt(efoId)
        }
      ),
      Field(
        "cohortShortName",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "cohortShortName").asOpt[String]
      ),
      Field(
        "diseaseModelAssociatedModelPhenotypes",
        OptionType(ListType(labelledElementImp)),
        description = None,
        resolve = js => (js.value \ "diseaseModelAssociatedModelPhenotypes").asOpt[Seq[JsValue]]
      ),
      Field(
        "diseaseModelAssociatedHumanPhenotypes",
        OptionType(ListType(labelledElementImp)),
        description = None,
        resolve = js => (js.value \ "diseaseModelAssociatedHumanPhenotypes").asOpt[Seq[JsValue]]
      ),
      Field(
        "significantDriverMethods",
        OptionType(ListType(StringType)),
        description = None,
        resolve = js => (js.value \ "significantDriverMethods").asOpt[Seq[String]]
      ),
      Field(
        "pValueExponent",
        OptionType(LongType),
        description = None,
        resolve = js => (js.value \ "pValueExponent").asOpt[Long]
      ),
      Field(
        "log2FoldChangePercentileRank",
        OptionType(LongType),
        description = None,
        resolve = js => (js.value \ "log2FoldChangePercentileRank").asOpt[Long]
      ),
      Field(
        "biologicalModelAllelicComposition",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "biologicalModelAllelicComposition").asOpt[String]
      ),
      Field(
        "confidence",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "confidence").asOpt[String]
      ),
      Field(
        "clinicalPhase",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "clinicalPhase").asOpt[Double]
      ),
      Field(
        "resourceScore",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "resourceScore").asOpt[Double]
      ),
      Field(
        "variantFunctionalConsequence",
        OptionType(sequenceOntologyTermImp),
        description = None,
        resolve = js => {
          val soId = ((js.value \ "variantFunctionalConsequenceId")
            .asOpt[String])
            .map(id => id.replace("_", ":"))
          logger.error(s"Finding variant functional consequence: $soId")
          soTermsFetcher.deferOpt(soId)
        }
      ),
      Field(
        "variantFunctionalConsequenceFromQtlId",
        OptionType(sequenceOntologyTermImp),
        description = None,
        resolve = js => {
          val soId = ((js.value \ "variantFunctionalConsequenceFromQtlId")
            .asOpt[String])
            .map(id => id.replace("_", ":"))
          soTermsFetcher.deferOpt(soId)
        }
      ),
      Field(
        "biologicalModelGeneticBackground",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "biologicalModelGeneticBackground").asOpt[String]
      ),
      Field(
        "urls",
        OptionType(ListType(labelledUriImp)),
        description = None,
        resolve = js => (js.value \ "urls").asOpt[Seq[JsValue]]
      ),
      Field(
        "literature",
        OptionType(ListType(StringType)),
        description = Some("list of pub med publications ids"),
        resolve = js => (js.value \ "literature").asOpt[Seq[String]]
      ),
      Field(
        "pubMedCentralIds",
        OptionType(ListType(StringType)),
        description = Some("list of central pub med publications ids"),
        resolve = js => (js.value \ "pmcIds").asOpt[Seq[String]]
      ),
      Field(
        "studyCases",
        OptionType(LongType),
        description = None,
        resolve = js => (js.value \ "studyCases").asOpt[Long]
      ),
      Field(
        "studyOverview",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "studyOverview").asOpt[String]
      ),
      Field(
        "allelicRequirements",
        OptionType(ListType(StringType)),
        description = None,
        resolve = js => (js.value \ "allelicRequirements").asOpt[Seq[String]]
      ),
      Field(
        "datasourceId",
        StringType,
        description = None,
        resolve = js => (js.value \ "datasourceId").as[String]
      ),
      Field(
        "datatypeId",
        StringType,
        description = None,
        resolve = js => (js.value \ "datatypeId").as[String]
      ),
      Field(
        "oddsRatioConfidenceIntervalUpper",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "oddsRatioConfidenceIntervalUpper").asOpt[Double]
      ),
      Field(
        "clinicalStatus",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "clinicalStatus").asOpt[String]
      ),
      Field(
        "log2FoldChangeValue",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "log2FoldChangeValue").asOpt[Double]
      ),
      Field(
        "oddsRatio",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "oddsRatio").asOpt[Double]
      ),
      Field(
        "cohortDescription",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "cohortDescription").asOpt[String]
      ),
      Field(
        "publicationYear",
        OptionType(LongType),
        description = None,
        resolve = js => (js.value \ "publicationYear").asOpt[Long]
      ),
      Field(
        "diseaseFromSource",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "diseaseFromSource").asOpt[String]
      ),
      Field(
        "diseaseFromSourceId",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "diseaseFromSourceId").asOpt[String]
      ),
      Field(
        "targetFromSourceId",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "targetFromSourceId").asOpt[String]
      ),
      Field(
        "targetModulation",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "targetModulation").asOpt[String]
      ),
      Field(
        "textMiningSentences",
        OptionType(ListType(evidenceTextMiningSentenceImp)),
        description = None,
        resolve = js => (js.value \ "textMiningSentences").asOpt[Seq[JsValue]]
      ),
      Field(
        "studyId",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "studyId").asOpt[String]
      ),
      Field(
        "clinicalSignificances",
        OptionType(ListType(StringType)),
        description = None,
        resolve = js => (js.value \ "clinicalSignificances").asOpt[Seq[String]]
      ),
      Field(
        "cohortId",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "cohortId").asOpt[String]
      ),
      Field(
        "pValueMantissa",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "pValueMantissa").asOpt[Double]
      ),
      Field(
        "pathways",
        OptionType(ListType(pathwayTermImp)),
        description = None,
        resolve = js => (js.value \ "pathways").asOpt[Seq[JsValue]]
      ),
      Field(
        "publicationFirstAuthor",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "publicationFirstAuthor").asOpt[String]
      ),
      Field(
        "alleleOrigins",
        OptionType(ListType(StringType)),
        description = None,
        resolve = js => (js.value \ "alleleOrigins").asOpt[Seq[String]]
      ),
      Field(
        "biologicalModelId",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "biologicalModelId").asOpt[String]
      ),
      Field(
        "biosamplesFromSource",
        OptionType(ListType(StringType)),
        description = None,
        resolve = js => (js.value \ "biosamplesFromSource").asOpt[Seq[String]]
      ),
      Field(
        "diseaseFromSourceMappedId",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "diseaseFromSourceMappedId").asOpt[String]
      ),
      Field(
        "beta",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "beta").asOpt[Double]
      ),
      Field(
        "betaConfidenceIntervalLower",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "betaConfidenceIntervalLower").asOpt[Double]
      ),
      Field(
        "betaConfidenceIntervalUpper",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "betaConfidenceIntervalUpper").asOpt[Double]
      ),
      Field(
        "studyStartDate",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "studyStartDate").asOpt[String]
      ),
      Field(
        "studyStopReason",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "studyStopReason").asOpt[String]
      ),
      Field(
        "studyStopReasonCategories",
        OptionType(ListType(StringType)),
        description =
          Some("Predicted reason(s) why the study has been stopped based on studyStopReason"),
        resolve = js => (js.value \ "studyStopReasonCategories").asOpt[Seq[String]]
      ),
      Field(
        "targetFromSource",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "targetFromSource").asOpt[String]
      ),
      Field(
        "cellLineBackground",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "cellLineBackground").asOpt[String]
      ),
      Field(
        "contrast",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "contrast").asOpt[String]
      ),
      Field(
        "crisprScreenLibrary",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "crisprScreenLibrary").asOpt[String]
      ),
      Field(
        "cellType",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "cellType").asOpt[String]
      ),
      Field(
        "statisticalTestTail",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "statisticalTestTail").asOpt[String]
      ),
      Field(
        "interactingTargetFromSourceId",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "interactingTargetFromSourceId").asOpt[String]
      ),
      Field(
        "phenotypicConsequenceLogFoldChange",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "phenotypicConsequenceLogFoldChange").asOpt[Double]
      ),
      Field(
        "phenotypicConsequenceFDR",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "phenotypicConsequenceFDR").asOpt[Double]
      ),
      Field(
        "phenotypicConsequencePValue",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "phenotypicConsequencePValue").asOpt[Double]
      ),
      Field(
        "geneticInteractionScore",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "geneticInteractionScore").asOpt[Double]
      ),
      Field(
        "geneticInteractionPValue",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "geneticInteractionPValue").asOpt[Double]
      ),
      Field(
        "geneticInteractionFDR",
        OptionType(FloatType),
        description = None,
        resolve = js => (js.value \ "geneticInteractionFDR").asOpt[Double]
      ),
      Field(
        "biomarkerList",
        OptionType(ListType(nameAndDescriptionImp)),
        description = None,
        resolve = js => (js.value \ "biomarkerList").asOpt[Seq[NameAndDescription]]
      ),
      Field(
        "projectDescription",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "projectDescription").asOpt[String]
      ),
      Field(
        "geneInteractionType",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "geneInteractionType").asOpt[String]
      ),
      Field(
        "targetRole",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "targetRole").asOpt[String]
      ),
      Field(
        "interactingTargetRole",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "interactingTargetRole").asOpt[String]
      ),
      Field(
        "ancestry",
        OptionType(StringType),
        description = Some("Genetic origin of a population"),
        resolve = js => (js.value \ "ancestry").asOpt[String]
      ),
      Field(
        "ancestryId",
        OptionType(StringType),
        description = Some("Identifier of the ancestry in the HANCESTRO ontology"),
        resolve = js => (js.value \ "ancestryId").asOpt[String]
      ),
      Field(
        "statisticalMethod",
        OptionType(StringType),
        description = Some("The statistical method used to calculate the association"),
        resolve = js => (js.value \ "statisticalMethod").asOpt[String]
      ),
      Field(
        "statisticalMethodOverview",
        OptionType(StringType),
        description = Some("Overview of the statistical method used to calculate the association"),
        resolve = js => (js.value \ "statisticalMethodOverview").asOpt[String]
      ),
      Field(
        "studyCasesWithQualifyingVariants",
        OptionType(LongType),
        description = Some(
          "Number of cases in a case-control study that carry at least one allele of the qualifying variant"
        ),
        resolve = js => (js.value \ "studyCasesWithQualifyingVariants").asOpt[Long]
      ),
      Field(
        "releaseVersion",
        OptionType(StringType),
        description = Some("Release version"),
        resolve = js => (js.value \ "releaseVersion").asOpt[String]
      ),
      Field(
        "releaseDate",
        OptionType(StringType),
        description = Some("Release date"),
        resolve = js => (js.value \ "releaseDate").asOpt[String]
      ),
      Field(
        "warningMessage",
        OptionType(StringType),
        description = Some("Warning message"),
        resolve = js => (js.value \ "warningMessage").asOpt[String]
      ),
      Field(
        "variantEffect",
        OptionType(StringType),
        description = Some("Variant effect"),
        resolve = js => (js.value \ "variantEffect").asOpt[String]
      ),
      Field(
        "directionOnTrait",
        OptionType(StringType),
        description = Some("Direction On Trait"),
        resolve = js => (js.value \ "directionOnTrait").asOpt[String]
      ),
      Field(
        "assessments",
        OptionType(ListType(StringType)),
        description = Some("Assessments"),
        resolve = js => (js.value \ "assessments").asOpt[Seq[String]]
      ),
      Field(
        "primaryProjectHit",
        OptionType(BooleanType),
        description = Some("Primary Project Hit"),
        resolve = js => (js.value \ "primaryProjectHit").asOpt[Boolean]
      ),
      Field(
        "primaryProjectId",
        OptionType(StringType),
        description = Some("Primary Project Id"),
        resolve = js => (js.value \ "primaryProjectId").asOpt[String]
      ),
      Field(
        "assays",
        OptionType(ListType(assaysImp)),
        description = None,
        resolve = js => (js.value \ "assays").asOpt[Seq[JsValue]]
      )
    )
  )
}
