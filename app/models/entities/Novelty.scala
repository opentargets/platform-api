package models.entities

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult
import utils.db.DbJsonParser.fromPositionedResult

case class Novelty(
    diseaseId: String,
    targetId: String,
    aggregationType: String,
    aggregationValue: String,
    year: Option[Int],
    associationScore: Double,
    novelty: Option[Double],
    yearlyEvidenceCount: Option[Int],
    isDirect: Boolean,
    meta_total: Long
)

case class NoveltyResults(
    count: Long,
    rows: Vector[Novelty]
)

object NoveltyResults {
  val empty: NoveltyResults = NoveltyResults(0, Vector.empty)
  implicit val getNoveltyRowFromDB: GetResult[Novelty] =
    GetResult(fromPositionedResult[Novelty])
  implicit val NoveltyImp: OFormat[Novelty] = Json.format[Novelty]
  implicit val NoveltyResultsImp: OFormat[NoveltyResults] = Json.format[NoveltyResults]
}
