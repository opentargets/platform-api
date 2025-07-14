package models.entities

import clickhouse.rep.SeqRep._
import models.entities.Configuration._
import play.api.libs.json.Json
import slick.jdbc.GetResult
import play.api.libs.json.OFormat

case class ResourceScore(name: String, value: Double)

case class Interval(
    chromosome: String,
    start: Long,
    end: Long,
    geneId: String,
    biosampleName: String,
    intervalType: String,
    score: Double,
    resourceScore: Vector[ResourceScore],
    datasourceId: String,
    pmid: String
)

case class Intervals(
    count: Long,
    rows: Vector[Interval]
)

object Intervals {
  val empty: Intervals = Intervals(0, Vector.empty)

  implicit val getIntervalRowFromDB: GetResult[Interval] =
    GetResult { r =>
      val chromosome: String = r.<<
      val start: Long = r.<<
      val end: Long = r.<<
      val geneId: String = r.<<
      val biosampleName: String = r.<<
      val intervalType: String = r.<<
      val score: Double = r.<<
      val resourceScores: String = r.<<
      val datasourceId: String = r.<<
      val pmid: String = r.<<

      Interval(
        chromosome,
        start,
        end,
        geneId,
        biosampleName,
        intervalType,
        score,
        TupleSeqRep[ResourceScore](
          resourceScores,
          tuple => {
            val tokens = tuple.split(",")
            val left = tokens(0)
            val right = tokens(1).toDouble
            ResourceScore(left, right)
          }
        ).rep,
        datasourceId,
        pmid
      )
    }

  implicit val IntervalImp: OFormat[Interval] = Json.format[Interval]
  implicit val ResourceScoreTypeImp: OFormat[ResourceScore] = Json.format[ResourceScore]
}
