package models.entities

import models.Backend
import models.gql.Fetchers.{
  diseasesFetcher,
  drugsFetcher,
  goFetcher,
  soTermsFetcher,
  targetsFetcher
}
import models.gql.Objects
import models.gql.Objects.{diseaseImp, drugImp, geneOntologyTermImp, targetImp}
import play.api.libs.json._
import sangria.schema.{
  Field,
  FloatType,
  ListType,
  LongType,
  ObjectType,
  OptionType,
  StringType,
  fields
}

object Evidence {
  val pathwayTermImp = ObjectType(
    "Pathway",
    "Pathway entry",
    fields[Backend, JsValue](
      Field("id",
            StringType,
            description = Some("Pathway ID"),
            resolve = js => (js.value \ "id").as[String]),
      Field("name",
            StringType,
            description = Some("Pathway Name"),
            resolve = js => (js.value \ "name").as[String])
    )
  )

  val sequenceOntologyTermImp = ObjectType(
    "SequenceOntologyTerm",
    "Sequence Ontology Term",
    fields[Backend, JsValue](
      Field("id",
            StringType,
            description = Some("Sequence Ontology ID"),
            resolve = js => (js.value \ "id").as[String]),
      Field("label",
            StringType,
            description = Some("Sequence Ontology Label"),
            resolve = js => (js.value \ "label").as[String])
    )
  )

  val evidenceTextMiningSentenceImp = ObjectType(
    "EvidenceTextMiningSentence",
    fields[Backend, JsValue](
      Field("dEnd", LongType, description = None, resolve = js => (js.value \ "dEnd").as[Long]),
      Field("tEnd", LongType, description = None, resolve = js => (js.value \ "tEnd").as[Long]),
      Field("dStart", LongType, description = None, resolve = js => (js.value \ "dStart").as[Long]),
      Field("tStart", LongType, description = None, resolve = js => (js.value \ "tStart").as[Long]),
      Field("section",
            StringType,
            description = None,
            resolve = js => (js.value \ "section").as[String]),
      Field("text", StringType, description = None, resolve = js => (js.value \ "text").as[String])
    )
  )
  val evidenceDiseaseCellLineImp = ObjectType(
    "DiseaseCellLine",
    fields[Backend, JsValue](
      Field("id", StringType, description = None, resolve = js => (js.value \ "id").as[String]),
      Field("name", StringType, description = None, resolve = js => (js.value \ "name").as[String]),
      Field("tissue", StringType, description = None, resolve = js => (js.value \ "tissue").as[String]),
      Field("tissueId", StringType, description = None, resolve = js => (js.value \ "tissueId").as[String]),
    )
  )

  val evidenceVariationImp = ObjectType(
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
      Field("numberMutatedSamples",
            OptionType(LongType),
            description = None,
            resolve = js => (js.value \ "numberMutatedSamples").asOpt[Long]),
      Field("numberSamplesTested",
            OptionType(LongType),
            description = None,
            resolve = js => (js.value \ "numberSamplesTested").asOpt[Long]),
      Field("numberSamplesWithMutationType",
            OptionType(LongType),
            description = None,
            resolve = js => (js.value \ "numberSamplesWithMutationType").asOpt[Long])
    )
  )

  val labelledElementImp = ObjectType(
    "LabelledElement",
    fields[Backend, JsValue](
      Field("id", StringType, description = None, resolve = js => (js.value \ "id").as[String]),
      Field("label",
            StringType,
            description = None,
            resolve = js => (js.value \ "label").as[String])
    )
  )

  val labelledUriImp = ObjectType(
    "LabelledUri",
    fields[Backend, JsValue](
      Field("url", StringType, description = None, resolve = js => (js.value \ "url").as[String]),
      Field("niceName",
            StringType,
            description = None,
            resolve = js => (js.value \ "niceName").as[String])
    )
  )

  val biomarkerGeneExpressionImp = ObjectType(
    "geneExpression",
    fields[Backend, JsValue](
      Field("name",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "name").asOpt[String]),
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
  val biomarkerVariantImp = ObjectType(
    "variant",
    fields[Backend, JsValue](
      Field("id",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "id").asOpt[String]),
      Field("name",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "name").asOpt[String]),
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
      Field("variant",
            OptionType(ListType(biomarkerVariantImp)),
            description = None,
            resolve = js => (js.value \ "variant").asOpt[Seq[JsValue]])
    )
  )

  val evidenceImp = ObjectType(
    "Evidence",
    "Evidence for a Target-Disease pair",
    fields[Backend, JsValue](
      Field("id",
            StringType,
            description = Some("Evidence identifier"),
            resolve = js => (js.value \ "id").as[String]),
      Field("score",
            FloatType,
            description = Some("Evidence score"),
            resolve = js => (js.value \ "score").as[Double]),
      Field("target", targetImp, description = Some("Target evidence"), resolve = js => {
        val tId = (js.value \ "targetId").as[String]
        targetsFetcher.defer(tId)
      }),
      Field("disease", diseaseImp, description = Some("Disease evidence"), resolve = js => {
        val dId = (js.value \ "diseaseId").as[String]
        diseasesFetcher.defer(dId)
      }),
      Field("biomarkerName",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "biomarkerName").asOpt[String]),
      Field("biomarkers",
            OptionType(biomarkersImp),
            description = None,
            resolve = js => (js.value \ "biomarkers").asOpt[JsValue]),
      Field("diseaseCellLines",
            OptionType(ListType(evidenceDiseaseCellLineImp)),
            description = None,
            resolve = js => (js.value \ "diseaseCellLines").asOpt[Seq[JsValue]]),
      Field("cohortPhenotypes",
            OptionType(ListType(StringType)),
            description = None,
            resolve = js => (js.value \ "cohortPhenotypes").asOpt[Seq[String]]),
      Field("targetInModel",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "targetInModel").asOpt[String]),
      Field("reactionId",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "reactionId").asOpt[String]),
      Field("reactionName",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "reactionName").asOpt[String]),
      Field("projectId",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "projectId").asOpt[String]),
      Field("variantId",
            OptionType(StringType),
            description = Some("Variant evidence"),
            resolve = js => (js.value \ "variantId").asOpt[String]),
      Field("variantRsId",
            OptionType(StringType),
            description = Some("Variant dbSNP identifier"),
            resolve = js => (js.value \ "variantRsId").asOpt[String]),
      Field(
        "oddsRatioConfidenceIntervalLower",
        OptionType(FloatType),
        description = Some("Confidence interval lower-bound  "),
        resolve = js => (js.value \ "oddsRatioConfidenceIntervalLower").asOpt[Double]
      ),
      Field("studySampleSize",
            OptionType(LongType),
            description = Some("Sample size"),
            resolve = js => (js.value \ "studySampleSize").asOpt[Long]),
      Field(
        "variantAminoacidDescriptions",
        OptionType(ListType(StringType)),
        description = None,
        resolve = js => (js.value \ "variantAminoacidDescriptions").asOpt[Seq[String]]
      ),
      Field("mutatedSamples",
            OptionType(ListType(evidenceVariationImp)),
            description = None,
            resolve = js => (js.value \ "mutatedSamples").asOpt[Seq[JsValue]]),
      Field("drug", OptionType(drugImp), description = None, resolve = js => {
        val drugId = (js.value \ "drugId").asOpt[String]
        drugsFetcher.deferOpt(drugId)
      }),
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
      Field("cohortShortName",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "cohortShortName").asOpt[String]),
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
      Field("significantDriverMethods",
            OptionType(ListType(StringType)),
            description = None,
            resolve = js => (js.value \ "significantDriverMethods").asOpt[Seq[String]]),
      Field("pValueExponent",
            OptionType(LongType),
            description = None,
            resolve = js => (js.value \ "pValueExponent").asOpt[Long]),
      Field("log2FoldChangePercentileRank",
            OptionType(LongType),
            description = None,
            resolve = js => (js.value \ "log2FoldChangePercentileRank").asOpt[Long]),
      Field("biologicalModelAllelicComposition",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "biologicalModelAllelicComposition").asOpt[String]),
      Field("confidence",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "confidence").asOpt[String]),
      Field("clinicalPhase",
            OptionType(LongType),
            description = None,
            resolve = js => (js.value \ "clinicalPhase").asOpt[Long]),
      Field("resourceScore",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "resourceScore").asOpt[Double]),
      Field(
        "variantFunctionalConsequence",
        OptionType(sequenceOntologyTermImp),
        description = None,
        resolve = js => {
          val soId = ((js.value \ "variantFunctionalConsequenceId")
            .asOpt[String])
            .map(id => id.replace("_", ":"))
          soTermsFetcher.deferOpt(soId)
        }
      ),
      Field("biologicalModelGeneticBackground",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "biologicalModelGeneticBackground").asOpt[String]),
      Field("urls",
            OptionType(ListType(labelledUriImp)),
            description = None,
            resolve = js => (js.value \ "urls").asOpt[Seq[JsValue]]),
      Field("literature",
            OptionType(ListType(StringType)),
            description = None,
            resolve = js => (js.value \ "literature").asOpt[Seq[String]]),
      Field("studyCases",
            OptionType(LongType),
            description = None,
            resolve = js => (js.value \ "studyCases").asOpt[Long]),
      Field("studyOverview",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "studyOverview").asOpt[String]),
      Field("allelicRequirements",
            OptionType(ListType(StringType)),
            description = None,
            resolve = js => (js.value \ "allelicRequirements").asOpt[Seq[String]]),
      Field("datasourceId",
            StringType,
            description = None,
            resolve = js => (js.value \ "datasourceId").as[String]),
      Field("datatypeId",
            StringType,
            description = None,
            resolve = js => (js.value \ "datatypeId").as[String]),
      Field("oddsRatioConfidenceIntervalUpper",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "oddsRatioConfidenceIntervalUpper").asOpt[Double]),
      Field("clinicalStatus",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "clinicalStatus").asOpt[String]),
      Field("log2FoldChangeValue",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "log2FoldChangeValue").asOpt[Double]),
      Field("oddsRatio",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "oddsRatio").asOpt[Double]),
      Field("cohortDescription",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "cohortDescription").asOpt[String]),
      Field("publicationYear",
            OptionType(LongType),
            description = None,
            resolve = js => (js.value \ "publicationYear").asOpt[Long]),
      Field("diseaseFromSource",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "diseaseFromSource").asOpt[String]),
      Field("diseaseFromSourceId",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "diseaseFromSourceId").asOpt[String]),
      Field("targetFromSourceId",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "targetFromSourceId").asOpt[String]),
      Field("targetModulation",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "targetModulation").asOpt[String]),
      Field(
        "textMiningSentences",
        OptionType(ListType(evidenceTextMiningSentenceImp)),
        description = None,
        resolve = js => (js.value \ "textMiningSentences").asOpt[Seq[JsValue]]
      ),
      Field("studyId",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "studyId").asOpt[String]),
      Field("clinicalSignificances",
            OptionType(ListType(StringType)),
            description = None,
            resolve = js => (js.value \ "clinicalSignificances").asOpt[Seq[String]]),
      Field("cohortId",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "cohortId").asOpt[String]),
      Field("pValueMantissa",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "pValueMantissa").asOpt[Double]),
      Field("pathways",
            OptionType(ListType(pathwayTermImp)),
            description = None,
            resolve = js => (js.value \ "pathways").asOpt[Seq[JsValue]]),
      Field("publicationFirstAuthor",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "publicationFirstAuthor").asOpt[String]),
      Field("alleleOrigins",
            OptionType(ListType(StringType)),
            description = None,
            resolve = js => (js.value \ "alleleOrigins").asOpt[Seq[String]]),
      Field("biologicalModelId",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "biologicalModelId").asOpt[String]),
      Field("biosamplesFromSource",
            OptionType(ListType(StringType)),
            description = None,
            resolve = js => (js.value \ "biosamplesFromSource").asOpt[Seq[String]]),
      Field("diseaseFromSourceMappedId",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "diseaseFromSourceMappedId").asOpt[String]),
      Field("beta",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "beta").asOpt[Double]),
      Field("betaConfidenceIntervalLower",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "betaConfidenceIntervalLower").asOpt[Double]),
      Field("betaConfidenceIntervalUpper",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "betaConfidenceIntervalUpper").asOpt[Double]),
      Field("studyStartDate",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "studyStartDate").asOpt[String]),
      Field("studyStopReason",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "studyStopReason").asOpt[String]),
      Field("targetFromSource",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "targetFromSource").asOpt[String]),
      Field("contrast",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "contrast").asOpt[String]),
      Field("crisprScreenLibrary",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "crisprScreenLibrary").asOpt[String]),
      Field("cellType",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "cellType").asOpt[String]),
      Field("cellLineBackground",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "cellLineBackground").asOpt[String]),
      Field("statisticalTestTail",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "statisticalTestTail").asOpt[String]),
      Field("interactingTargetFromSourceId",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "interactingTargetFromSourceId").asOpt[String]),
      Field("geneticInteractionMethod",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "geneticInteractionMethod").asOpt[String]),
      Field("phenotypicConsequenceLogFoldChange",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "phenotypicConsequenceLogFoldChange").asOpt[Double]),
      Field("phenotypicConsequenceFDR",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "phenotypicConsequenceFDR").asOpt[Double]),
      Field("phenotypicConsequencePValue",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "phenotypicConsequencePValue").asOpt[Double]),
      Field("geneticInteractionScore",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "geneticInteractionScore").asOpt[Double]),
      Field("geneticInteractionPValue",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "geneticInteractionPValue").asOpt[Double]),
      Field("geneticInteractionFDR",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "geneticInteractionFDR").asOpt[Double]),
    )
  )
}
