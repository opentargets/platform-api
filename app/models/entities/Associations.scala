package models.entities

import clickhouse.rep.SeqRep._
import clickhouse.rep.SeqRep.Implicits._
import models.entities.Configuration.{DatasourceSettings, LUTableSettings}
import models.entities.Configuration.JSONImplicits._
import models.entities.Network.JSONImplicits._
import play.api.Logger
import play.api.libs.json.Json
import slick.jdbc.GetResult

case class ScoredComponent(id: String, score: Double)

/**
 * this is one side of an full association as the other part is fixed. In this
 * case those are T <-> D and an association is built based on a harmonic computation
 * where the overall score is `score` and each datasource contribution is contained
 * in `scorePerDS` vector
 */
case class Association(id: String, score: Double,
                       datatypeScores: Vector[ScoredComponent],
                       datasourceScores: Vector[ScoredComponent])

case class Associations(datasources: Seq[DatasourceSettings],
                        count: Long,
                        rows: Vector[Association])

case class EvidenceSource(datasource: String, datatype: String)

object Associations {
  val empty = Associations(Seq.empty, 0, Vector.empty)

  object DBImplicits {
    val logger = Logger(this.getClass)

    implicit val getAssociationOTFRowFromDB: GetResult[Association] = {

      GetResult(r => {
        val id: String = r.<<
        val score: Double = r.<<
        val tuples1: String = r.<<
        val tuples2: String = r.<<

        Association(id, score,
          TupleSeqRep[ScoredComponent](tuples1, tuple => {
            val tokens = tuple.split(",")
            val left = parseFastString(tokens(0))
            val right = tokens(1).toDouble
            ScoredComponent(left, right)
          }).rep,
          TupleSeqRep[ScoredComponent](tuples2, tuple => {
            val tokens = tuple.split(",")
            val left = parseFastString(tokens(0))
            val right = tokens(1).toDouble
            ScoredComponent(left, right)
          }).rep
        )
      })
    }

    implicit val getEvidenceSourceFromDB: GetResult[EvidenceSource] =
      GetResult(r => EvidenceSource(r.<<, r.<<))
  }

  object JSONImplicits {
    implicit val scoredDataTypeImp = Json.format[ScoredComponent]
    implicit val AssociationOTFImp = Json.format[Association]
  }

}
