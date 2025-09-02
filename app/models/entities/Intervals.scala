package models.entities

import clickhouse.rep.SeqRep._
import models.entities.Configuration._
import play.api.libs.json.Json
import slick.jdbc.GetResult
import play.api.libs.json.OFormat

case class ResourceScore(name: String, value: Double)

case class Interval(
    chromosome: String,
    start: Int,
    end: Int,
    geneId: String,
    biosampleName: String,
    biosampleId: String,
    intervalType: String,
    distanceToTss: Int,
    score: Double,
    resourceScore: Vector[ResourceScore],
    datasourceId: String,
    pmid: String,
    studyId: String
)

case class Intervals(
    count: Long,
    rows: Vector[Interval]
)

object Intervals {
  val empty: Intervals = Intervals(0, Vector.empty)
  implicit val getIntervalRowFromDB: GetResult[Interval] =
    GetResult(r => Json.parse(r.<<[String]).as[Interval])
  implicit val IntervalImp: OFormat[Interval] = Json.format[Interval]
  implicit val ResourceScoreTypeImp: OFormat[ResourceScore] = Json.format[ResourceScore]
}
