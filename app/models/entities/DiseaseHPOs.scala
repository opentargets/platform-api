package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import models.gql.TypeWithId
import slick.jdbc.GetResult

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
    references: Seq[String],
    sex: Option[String],
    resource: String
)

case class DiseaseHPO(phenotype: String, disease: String, evidence: Seq[DiseaseHPOEvidences])

case class DiseaseHPOs(count: Long, rows: IndexedSeq[DiseaseHPO], id: String = "")
    extends TypeWithId

object DiseaseHPOs {
  implicit val diseaseHPOsImpGetResult: GetResult[DiseaseHPOs] =
    GetResult(r => Json.parse(r.<<[String]).as[DiseaseHPOs])
  def empty: DiseaseHPOs = DiseaseHPOs(0, IndexedSeq.empty)
  implicit val diseaseHPOsImpF: OFormat[DiseaseHPOs] = Json.format[models.entities.DiseaseHPOs]
  implicit val diseaseHPOImpF: OFormat[DiseaseHPO] = Json.format[models.entities.DiseaseHPO]
  implicit val diseaseHPOEvidencesImpF: OFormat[DiseaseHPOEvidences] =
    Json.format[models.entities.DiseaseHPOEvidences]
}
