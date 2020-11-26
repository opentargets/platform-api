package models.entities

import models.Backend
import models.gql.Fetchers.{diseasesFetcher, drugsFetcher, soTermsFetcher, targetsFetcher}
import models.gql.Fetchers.{diseasesFetcher, drugsFetcher, soTermsFetcher, targetsFetcher}
import models.gql.Objects.{diseaseImp, drugImp, targetImp}
import play.api.libs.json._
import sangria.schema.{Field, FloatType, ListType, LongType, ObjectType, OptionType, StringType, fields}

object Evidence {
  val sequenceOntologyTermImp = ObjectType("SequenceOntologyTerm",
    "Sequence Ontology Term",
    fields[Backend, JsValue](
      Field("id", StringType, description = Some("Sequence Ontology ID"), resolve = js => (js.value \ "id").as[String]),
      Field("label", StringType, description = Some("Sequence Ontology Label"), resolve = js => (js.value \ "label").as[String])
    ))

  val evidenceTextMiningSentenceImp = ObjectType("EvidenceTextMiningSentence",
    fields[Backend, JsValue](
      Field("dEnd", LongType, description = None, resolve = js => (js.value \ "dEnd").as[Long]),
      Field("tEnd", LongType, description = None, resolve = js => (js.value \ "tEnd").as[Long]),
      Field("dStart", LongType, description = None, resolve = js => (js.value \ "dStart").as[Long]),
      Field("tStart", LongType, description = None, resolve = js => (js.value \ "tStart").as[Long]),
      Field("section", StringType, description = None, resolve = js => (js.value \ "section").as[String]),
      Field("text", StringType, description = None, resolve = js => (js.value \ "text").as[String])
    ))

  val evidenceVariationImp = ObjectType("EvidenceVariation",
    "Sequence Ontology Term",
    fields[Backend, JsValue](
      Field("functionalConsequence", sequenceOntologyTermImp, description = None, resolve = js => {
        val soId = ((js.value \ "functionalConsequenceId").as[String]).replace("_", ":")
        soTermsFetcher.defer(soId)
      }),
      Field("aminoacidDescription", OptionType(StringType), description = None, resolve = js => (js.value \ "variantAminoacidDescription").asOpt[String]),
      Field("numberMutatedSamples", OptionType(LongType), description = None, resolve = js => (js.value \ "numberMutatedSamples").asOpt[Long]),
      Field("numberSamplesTested", OptionType(LongType), description = None, resolve = js => (js.value \ "numberSamplesTested").asOpt[Long]),
      Field("numberSamplesWithMutationType", OptionType(LongType), description = None, resolve = js => (js.value \ "numberSamplesWithMutationType").asOpt[Long])
    ))

  val labelledElementImp = ObjectType("LabelledElement",
    fields[Backend, JsValue](
      Field("id", StringType, description = None, resolve = js => (js.value \ "id").as[String]),
      Field("label", StringType, description = None, resolve = js => (js.value \ "label").as[String])
    ))

  val labelledUriImp = ObjectType("LabelledUri",
    fields[Backend, JsValue](
      Field("url", StringType, description = None, resolve = js => (js.value \ "url").as[String]),
      Field("niceName", StringType, description = None, resolve = js => (js.value \ "niceName").as[String])
    ))

  val evidenceImp = ObjectType("Evidence",
    "Evidence for a Target-Disease pair",
    fields[Backend, JsValue](
      Field("id", StringType, description = Some("Evidence identifier"), resolve = js => (js.value \ "id").as[String]),
      Field("score", FloatType, description = Some("Evidence score"), resolve = js => (js.value \ "score").as[Double]),
      Field("target", targetImp, description = Some("Target evidence"), resolve = js => {
        val tId = (js.value \ "targetId").as[String]
        targetsFetcher.defer(tId)
      }),
      Field("disease", diseaseImp, description = Some("Disease evidence"), resolve = js => {
        val dId = (js.value \ "diseaseId").as[String]
        diseasesFetcher.defer(dId)
      }),
      Field("diseaseCellLines", OptionType(ListType(StringType)), description = None, resolve = js => (js.value \ "diseaseCellLines").asOpt[Seq[String]]),
      Field("cohortPhenotypes", OptionType(ListType(StringType)), description = None, resolve = js => (js.value \ "cohortPhenotypes").asOpt[Seq[String]]),
      Field("targetInModel", OptionType(StringType), description = None, resolve = js => (js.value \ "targetInModel").asOpt[String]),
      Field("reactionId", OptionType(StringType), description = None, resolve = js => (js.value \ "reactionId").asOpt[String]),
      Field("variantId", OptionType(StringType), description = Some("Variant evidence"), resolve = js => (js.value \ "variantId").asOpt[String]),
      Field("variantRsId", OptionType(StringType), description = Some("Variant dbSNP identifier"), resolve = js => (js.value \ "variantRsId").asOpt[String]),
      Field("targetModulation", OptionType(StringType), description = None, resolve = js => (js.value \ "targetModulation").asOpt[String]),
      Field("confidenceIntervalLower", OptionType(FloatType), description = Some("Confidence interval lower-bound  "), resolve = js => (js.value \ "confidenceIntervalLower").asOpt[Double]),
      Field("studySampleSize", OptionType(LongType), description = Some("Sample size"), resolve = js => (js.value \ "studySampleSize").asOpt[Long]),
      Field("variants", OptionType(ListType(evidenceVariationImp)), description = None, resolve = js => (js.value \ "variants").asOpt[Seq[JsValue]]),
      Field("drug", OptionType(drugImp), description = None, resolve = js => {
        val drugId = (js.value \ "drugId").asOpt[String]
        drugsFetcher.deferOpt(drugId)
      }),
      Field("cohortShortName", OptionType(StringType), description = None, resolve = js => (js.value \ "cohortShortName").asOpt[String]),
      Field("diseaseModelAssociatedModelPhenotypes", OptionType(ListType(labelledElementImp)), description = None, resolve = js => (js.value \ "diseaseModelAssociatedModelPhenotypes").asOpt[Seq[JsValue]]),
      Field("diseaseModelAssociatedHumanPhenotypes", OptionType(ListType(labelledElementImp)), description = None, resolve = js => (js.value \ "diseaseModelAssociatedHumanPhenotypes").asOpt[Seq[JsValue]]),
      Field("significantDriverMethods", OptionType(ListType(StringType)), description = None, resolve = js => (js.value \ "significantDriverMethods").asOpt[Seq[String]]),
      Field("resourceScoreExponent", OptionType(LongType), description = None, resolve = js => (js.value \ "resourceScoreExponent").asOpt[Long]),
      Field("log2FoldChangePercentileRank", OptionType(LongType), description = None, resolve = js => (js.value \ "log2FoldChangePercentileRank").asOpt[Long]),
      Field("biologicalModelAllelicComposition", OptionType(StringType), description = None, resolve = js => (js.value \ "biologicalModelAllelicComposition").asOpt[String]),
      Field("confidence", OptionType(StringType), description = None, resolve = js => (js.value \ "confidence").asOpt[String]),
      Field("clinicalPhase", OptionType(LongType), description = None, resolve = js => (js.value \ "clinicalPhase").asOpt[Long]),
      Field("resourceScore", OptionType(FloatType), description = None, resolve = js => (js.value \ "resourceScore").asOpt[Double]),
      Field("variantFunctionalConsequence", OptionType(sequenceOntologyTermImp), description = None, resolve = js => {
        val soId = ((js.value \ "variantFunctionalConsequenceId").asOpt[String])
          .map(id => id.replace("_", ":"))
        soTermsFetcher.deferOpt(soId)
      }),
      Field("biologicalModelGeneticBackground", OptionType(StringType), description = None, resolve = js => (js.value \ "biologicalModelGeneticBackground").asOpt[String]),
      Field("clinicalUrls", OptionType(ListType(labelledUriImp)), description = None, resolve = js => (js.value \ "clinicalUrls").asOpt[Seq[JsValue]]),
      Field("literature", OptionType(ListType(StringType)), description = None, resolve = js => (js.value \ "literature").asOpt[Seq[String]]),
      Field("studyCases", OptionType(StringType), description = None, resolve = js => (js.value \ "studyCases").asOpt[String]),
      Field("studyOverview", OptionType(StringType), description = None, resolve = js => (js.value \ "studyOverview").asOpt[String]),
      Field("allelicRequirements", OptionType(ListType(StringType)), description = None, resolve = js => (js.value \ "allelicRequirements").asOpt[Seq[String]]),
      Field("pathwayName", OptionType(StringType), description = None, resolve = js => (js.value \ "pathwayName").asOpt[String]),
      Field("datasourceId", StringType, description = None, resolve = js => (js.value \ "datasourceId").as[String]),
      Field("datatypeId", StringType, description = None, resolve = js => (js.value \ "datatypeId").as[String]),
      Field("confidenceIntervalUpper", OptionType(FloatType), description = None, resolve = js => (js.value \ "confidenceIntervalUpper").asOpt[Double]),
      Field("clinicalStatus", OptionType(StringType), description = None, resolve = js => (js.value \ "clinicalStatus").asOpt[String]),
      Field("log2FoldChangeValue", OptionType(FloatType), description = None, resolve = js => (js.value \ "log2FoldChangeValue").asOpt[Double]),
      Field("oddsRatio", OptionType(FloatType), description = None, resolve = js => (js.value \ "oddsRatio").asOpt[Double]),
      Field("cohortDescription", OptionType(StringType), description = None, resolve = js => (js.value \ "cohortDescription").asOpt[String]),
      Field("publicationYear", OptionType(LongType), description = None, resolve = js => (js.value \ "publicationYear").asOpt[Long]),
      Field("diseaseFromSource", OptionType(StringType), description = None, resolve = js => (js.value \ "diseaseFromSource").asOpt[String]),
      Field("targetFromSource", OptionType(StringType), description = None, resolve = js => (js.value \ "targetFromSource").asOpt[String]),
      Field("targetModulation", OptionType(StringType), description = None, resolve = js => (js.value \ "targetModulation").asOpt[String]),

      Field("textMiningSentences", OptionType(ListType(evidenceTextMiningSentenceImp)), description = None, resolve = js => (js.value \ "textMiningSentences").asOpt[Seq[JsValue]]),
      Field("recordId", OptionType(StringType), description = None, resolve = js => (js.value \ "recordId").asOpt[String]),
      Field("studyId", OptionType(StringType), description = None, resolve = js => (js.value \ "studyId").asOpt[String]),
      Field("clinicalSignificances", OptionType(ListType(StringType)), description = None, resolve = js => (js.value \ "clinicalSignificances").asOpt[Seq[String]]),
      Field("cohortId", OptionType(StringType), description = None, resolve = js => (js.value \ "cohortId").asOpt[String]),
      Field("resourceScoreMantissa", OptionType(LongType), description = None, resolve = js => (js.value \ "resourceScoreMantissa").asOpt[Long]),
      Field("locus2GeneScore", OptionType(FloatType), description = None, resolve = js => (js.value \ "locus2GeneScore").asOpt[Double]),
      Field("pathwayId", OptionType(StringType), description = None, resolve = js => (js.value \ "pathwayId").asOpt[String]),
      Field("publicationFirstAuthor", OptionType(StringType), description = None, resolve = js => (js.value \ "publicationFirstAuthor").asOpt[String]),
      Field("contrast", OptionType(StringType), description = None, resolve = js => (js.value \ "contrast").asOpt[String])
    ))
}