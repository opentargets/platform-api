package models

import math._
import clickhouse.rep.SeqRep._
import play.api.libs.json.{Json, JsonConfiguration}
import slick.jdbc.GetResult

object Entities {
  case class TargetsBody(ids: Seq[String])

  case class Pagination(index: Int, size: Int) {
    def toSQL: String = (index, size) match {
      case (0, 0) => s"LIMIT ${Pagination.sizeDefault}"
      case (0, s) => s"LIMIT $s"
      case (i, 0) => s"LIMIT ${i * Pagination.sizeDefault}, ${Pagination.sizeDefault}"
      case (i, s) => s"LIMIT ${i * s} , $s"
      case _ => s"LIMIT ${Pagination.indexDefault}, ${Pagination.sizeDefault}"
    }
    def toES: (Int, Int) = (index, size) match {
      case (0, 0) => (0, Pagination.sizeDefault)
      case (0, s) => (0, s)
      case (i, 0)  => (i*Pagination.sizeDefault, Pagination.sizeDefault)
      case (i, s) => (i*s, s)
      case _ => (0, Pagination.sizeDefault)
    }
  }

  object Pagination {
    val sizeMax: Int = 10000
    val sizeDefault: Int = 25
    val indexDefault: Int = 0
    def mkDefault: Pagination = Pagination(indexDefault, sizeDefault)
  }

  object JSONImplicits {
    implicit val targetsBodyImp = Json.format[Entities.TargetsBody]
  }
}
