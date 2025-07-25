package models.entities

import clickhouse.rep.SeqRep._
import models.entities.Configuration._
import play.api.libs.json.Json
import slick.jdbc.GetResult
import doobie.util.Read
import doobie.util.Get
import cats.data.NonEmptyList
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import play.api.libs.json.OFormat
import cats.effect._

case class ResourceScore(name: String, value: Double)

case class Interval(
    chromosome: String,
    start: Int,
    end: Int,
    geneId: String,
    biosampleName: String,
    biosampleId: String,
    intervalType: String,
    distanceToTSS: Int,
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
  implicit val resourceScoreRead: Read[ResourceScore] =
    Read[(String, Double)].map { case (name, value) => ResourceScore(name, value) }
  implicit val resourceScoreGet: Get[Array[ResourceScore]] =
    Get.Advanced
      .array[ResourceScore](NonEmptyList.one("Array(Tuple(String, Double))"))

  implicit val intervalRead: Read[Interval] =
    Read[
      (String,
       Int,
       Int,
       String,
       String,
       String,
       String,
       Int,
       Double,
       Vector[ResourceScore],
       String,
       String,
       String
      )
    ]
      .map {
        case (chromosome,
              start,
              end,
              geneId,
              biosampleName,
              biosampleId,
              intervalType,
              distanceToTSS,
              score,
              resourceScores,
              datasourceId,
              pmid,
              studyId
            ) =>
          Interval(
            chromosome,
            start,
            end,
            geneId,
            biosampleName,
            biosampleId,
            intervalType,
            distanceToTSS,
            score,
            resourceScores,
            datasourceId,
            pmid,
            studyId
          )
      }
  // implicit val intervalsRead: Read[Intervals] =
  //   Read[(Long, Interval)].map { case (count, rows) => Intervals(count, Vector(rows)) }

  // implicit val getIntervalRowFromDB: GetResult[Interval] =
  //   GetResult { r =>
  //     val chromosome: String = r.<<
  //     val start: Int = r.<<
  //     val end: Int = r.<<
  //     val geneId: String = r.<<
  //     val biosampleName: String = r.<<
  //     val biosampleId: String = r.<<
  //     val intervalType: String = r.<<
  //     val distanceToTSS: Int = r.<<
  //     val score: Double = r.<<
  //     val resourceScores: String = r.<<
  //     val datasourceId: String = r.<<
  //     val pmid: String = r.<<
  //     val studyId: String = r.<<

  //     Interval(
  //       chromosome,
  //       start,
  //       end,
  //       geneId,
  //       biosampleName,
  //       biosampleId,
  //       intervalType,
  //       distanceToTSS,
  //       score,
  //       TupleSeqRep[ResourceScore](
  //         resourceScores,
  //         tuple => {
  //           val tokens = tuple.split(",")
  //           val left = tokens(0)
  //           val right = tokens(1).toDouble
  //           ResourceScore(left, right)
  //         }
  //       ).rep,
  //       datasourceId,
  //       pmid,
  //       studyId
  //     )
  //   }

  implicit val IntervalImp: OFormat[Interval] = Json.format[Interval]
  implicit val ResourceScoreTypeImp: OFormat[ResourceScore] = Json.format[ResourceScore]
}
