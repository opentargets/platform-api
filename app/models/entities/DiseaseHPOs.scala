package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class DiseaseHPOEvidences(
    aspect: Option[String],
    bioCuration: Option[String],
    diseaseFromSourceId: String,
    diseaseFromSource: String,
    diseaseName: String,
    evidence: Option[String],
    frequency: Option[String],
    modifier: Option[String],
    onset: Option[String],
    qualifierNot: Boolean,
    referenceId: Option[String],
    sex: Option[String],
    resource: String
)

case class DiseaseHPO(phenotype: String, disease: String, evidences: Seq[DiseaseHPOEvidences])

case class DiseaseHPOs(count: Long, rows: Seq[DiseaseHPO])

object DiseaseHPOs {

  implicit val diseaseHPOEvidencesImpW = Json.writes[models.entities.DiseaseHPOEvidences]
  implicit val diseaseHPOEvidencesImpR: Reads[models.entities.DiseaseHPOEvidences] =
    ( (JsPath \ "aspect").readNullable[String] and
      (JsPath \ "bioCuration").readNullable[String] and
      (JsPath \ "diseaseFromSourceId").read[String] and
      (JsPath \ "diseaseFromSource").read[String] and
      (JsPath \ "diseaseName").read[String] and
      (JsPath \ "evidence").readNullable[String] and
      (JsPath \ "frequency").readNullable[String] and
      (JsPath \ "modifier").readNullable[String] and
      (JsPath \ "onset").readNullable[String] and
      (JsPath \ "qualifierNot").read[Boolean] and
      (JsPath \ "referenceId").readNullable[String] and
      (JsPath \ "sex").readNullable[String] and
      (JsPath \ "resource").read[String]
    )(DiseaseHPOEvidences.apply _)

  implicit val diseaseHPOImpW = Json.writes[models.entities.DiseaseHPO]
  implicit val diseaseHPOImpR: Reads[models.entities.DiseaseHPO] =
    ((JsPath \ "phenotype").read[String] and
      (JsPath \ "disease").read[String] and
      (JsPath \ "evidences").readWithDefault[Seq[DiseaseHPOEvidences]](Seq.empty))(
      DiseaseHPO.apply _
    )

  implicit val diseaseHPOsImpF = Json.format[models.entities.DiseaseHPOs]
}
