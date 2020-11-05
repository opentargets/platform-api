package models.entities

import play.api.libs.json.Json

case class LabelledElement(id: String, label: String)

case class LabelledUri(url: String, niceName: String)

case class EvidenceVariation(functionalConsequenceId: String,
                             inheritancePattern: Option[String],
                             variantAminoacidDescription: Option[String],
                             numberMutatedSamples: Option[Long],
                             numberSamplesTested: Option[Long],
                             numberSamplesWithMutationType: Option[Long])

case class EvidenceTextMiningSentence(dEnd: Long, dStart: Long, section: String, tEnd: Long, tStart: Long, text: String)

object Evidence {
  implicit val evidenceTextMiningSentenceJSONImp = Json.format[EvidenceTextMiningSentence]
  implicit val evidenceVariationJSONImp = Json.format[EvidenceVariation]
  implicit val labelledUriJSONImp = Json.format[LabelledUri]
  implicit val labelledElementJSONImp = Json.format[LabelledElement]
}