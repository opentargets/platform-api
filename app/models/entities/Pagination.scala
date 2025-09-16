package models.entities

import play.api.libs.json.Json
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import models.entities.Violations.{
  PaginationSizeError,
  PaginationIndexError,
  InputParameterCheckError
}
import sangria.validation.BaseViolation
import scala.util.{Try, Failure, Success}

/** Pagination case class takes an index from 0..page-1 and size indicate the batch of each page.
  */
case class Pagination(index: Int, size: Int) {
  lazy val offset: Int = toES._1

  lazy val next: Pagination = this.copy(index = this.index + 1)

  def hasValidRange(maxSize: Int = Pagination.sizeMax): Boolean = size <= maxSize

  val offsetLimit: (Int, Int) = (index, size) match {
    case (0, 0) => (0, Pagination.sizeDefault)
    case (0, s) => (0, s)
    case (i, 0) => (i * Pagination.sizeDefault, Pagination.sizeDefault)
    case (i, s) => (i * s, s)
    case null   => (0, Pagination.sizeDefault)
  }

  val toES: (Int, Int) = (index, size) match {
    case (0, 0) => (0, Pagination.sizeDefault)
    case (0, s) => (0, s)
    case (i, 0) => (i * Pagination.sizeDefault, Pagination.sizeDefault)
    case (i, s) => (i * s, s)
    case null   => (0, Pagination.sizeDefault)
  }
}

object Pagination {
  val sizeMax: Int = 3000
  val sizeDefault: Int = 25
  val indexDefault: Int = 0

  def create(index: Int, size: Int): Try[Pagination] = createHelper(index, size, Vector.empty)
  def createHelper(
      index: Int,
      size: Int,
      errs: Vector[BaseViolation]
  ): Try[Pagination] = (index, size) match {
    case (i, s) if i < 0 =>
      createHelper(indexDefault, s, errs :+ PaginationIndexError(i))
    case (i, s) if s < 0 || s > sizeMax =>
      createHelper(i, sizeDefault, errs :+ PaginationSizeError(s, sizeMax))
    case _ =>
      if (errs.nonEmpty) Failure(InputParameterCheckError(errs))
      else Success(Pagination(index, size))
  }

  /** @return
    *   page with defaults: index = 0, size = 25.
    */
  def mkDefault: Pagination = Pagination(indexDefault, sizeDefault)
  def mkMax: Pagination = Pagination(Pagination.indexDefault, Pagination.sizeMax)

  implicit val paginationJSONImpR: Reads[Pagination] = (
    (__ \ "index").read[Int] and
      (__ \ "size").read[Int]
  )(Pagination.create).flatMap {
    case Success(p) => Reads(_ => JsSuccess(p))
    case Failure(e) => Reads(_ => JsError(e.getMessage))
  }
  implicit val paginationJSONImpW: Writes[Pagination] = Json.writes[Pagination]
}
