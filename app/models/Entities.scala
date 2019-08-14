package models

import clickhouse.rep.SeqRep._
import slick.jdbc.GetResult

import scala.math.pow

object Entities {
  case class Pagination(index: Int, size: Int) {
    def toSQL: String = (index, size) match {
      case (0, 0) => s"LIMIT ${Pagination.sizeDefault}"
      case (0, s) => s"LIMIT $s"
      case (i, 0) => s"LIMIT ${i * Pagination.sizeDefault}, ${Pagination.sizeDefault}"
      case (i, s) => s"LIMIT ${i * s} , $s"
      case _ => s"LIMIT ${Pagination.indexDefault}, ${Pagination.sizeDefault}"
    }
  }

  object Pagination {
    val sizeMax: Int = 10000
    val sizeDefault: Int = 100
    val indexDefault: Int = 0
    def mkDefault: Pagination = Pagination(indexDefault, sizeDefault)
  }
}

