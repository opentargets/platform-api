package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class DiseaseHPOEvidences(
    aspect: Option[String],
    bioCuration: Option[String],
    diseaseFromSourceId: String,
    diseaseFromSource: String,
    diseaseName: String,
    evidenceType: Option[String],
    frequency: Option[String],
    modifiers: Seq[String],
    onset: Seq[String],
    qualifierNot: Boolean,
    references:  Seq[String],
    sex: Option[String],
    resource: String
)

case class DiseaseHPO(phenotype: String, disease: String, evidence: Seq[DiseaseHPOEvidences])

case class DiseaseHPOs(count: Long, rows: Seq[DiseaseHPO])

object DiseaseHPOs {

  implicit val diseaseHPOEvidencesImpW: OWrites[DiseaseHPOEvidences] = Json.writes[models.entities.DiseaseHPOEvidences]
  implicit val diseaseHPOEvidencesImpR: Reads[models.entities.DiseaseHPOEvidences] =
    ((JsPath \ "aspect").readNullable[String] and
      (JsPath \ "bioCuration").readNullable[String] and
      (JsPath \ "diseaseFromSourceId").read[String] and
      (JsPath \ "diseaseFromSource").read[String] and
      (JsPath \ "diseaseName").read[String] and
      (JsPath \ "evidenceType").readNullable[String] and
      (JsPath \ "frequency").readNullable[String] and
      (JsPath \ "modifiers").readWithDefault[Seq[String]](Seq.empty) and
      (JsPath \ "onset").readWithDefault[Seq[String]](Seq.empty) and
      (JsPath \ "qualifierNot").read[Boolean] and
      (JsPath \ "references").readWithDefault[Seq[String]](Seq.empty) and
      (JsPath \ "sex").readNullable[String] and
      (JsPath \ "resource").read[String]
    )(DiseaseHPOEvidences.apply _)

  implicit val diseaseHPOImpW: OWrites[DiseaseHPO] = Json.writes[models.entities.DiseaseHPO]
  implicit val diseaseHPOImpR: Reads[models.entities.DiseaseHPO] =
    ((JsPath \ "phenotype").read[String] and
      (JsPath \ "disease").read[String] and
      (JsPath \ "evidence").readWithDefault[Seq[DiseaseHPOEvidences]](Seq.empty)) (
      DiseaseHPO.apply _
    )

  implicit val diseaseHPOsImpF: OFormat[DiseaseHPOs] = Json.format[models.entities.DiseaseHPOs]
}
