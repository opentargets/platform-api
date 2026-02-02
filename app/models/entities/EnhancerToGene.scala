package models.entities

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult

case class ResourceScore(name: String, value: Double)

case class EnhancerToGene(
    chromosome: String,
    start: Int,
    end: Int,
    geneId: String,
    biosampleName: String,
    biosampleId: String,
    biosampleFromSourceId: Option[String],
    intervalType: String,
    distanceToTss: Int,
    score: Double,
    resourceScore: Vector[ResourceScore],
    datasourceId: String,
    pmid: String,
    studyId: String,
    qualityControls: Option[Seq[String]],
    meta_total: Long
)

case class EnhancerToGenes(
    count: Long,
    rows: Vector[EnhancerToGene]
)

object EnhancerToGenes {
  val empty: EnhancerToGenes = EnhancerToGenes(0, Vector.empty)
  implicit val getEnhancerToGeneRowFromDB: GetResult[EnhancerToGene] =
    GetResult(r => Json.parse(r.<<[String]).as[EnhancerToGene])
  implicit val EnhancerToGeneImp: OFormat[EnhancerToGene] = Json.format[EnhancerToGene]
  implicit val ResourceScoreTypeImp: OFormat[ResourceScore] = Json.format[ResourceScore]
}
