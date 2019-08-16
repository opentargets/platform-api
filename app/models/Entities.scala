package models

import clickhouse.rep.SeqRep._
import play.api.libs.json.{Json, JsonConfiguration}
import slick.jdbc.GetResult

import scala.math.pow
import models.entities.Target
import play.api.libs.json.JsonNaming._

object Entities {
  case class MetaVersion(x: Int, y: Int, z: Int)
  case class Meta(name: String, version: MetaVersion)

  case class HealthCheck(ok: Boolean, status: String)

  case class ElasticsearchSettings(host: String, port: Int)

  case class TargetsBody(ids: Seq[String])

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

  object JSONImplicits {
    implicit val metaVersionImp = Json.format[Entities.MetaVersion]
    implicit val metaImp = Json.format[Entities.Meta]

    implicit val healthImp = Json.format[Entities.HealthCheck]

    implicit val esSettingsImp = Json.reads[Entities.ElasticsearchSettings]

    implicit val targetsBodyImp = Json.format[Entities.TargetsBody]
    implicit val apiErrorMessageImp = Json.format[entities.APIErrorMessage]
    implicit val targetConfig = JsonConfiguration(SnakeCase)
    implicit val targetImp = Json.format[models.entities.Target]
  }
}
