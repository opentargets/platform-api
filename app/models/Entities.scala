package models

import clickhouse.rep.SeqRep._
import play.api.libs.json.{Json, JsonConfiguration}
import slick.jdbc.GetResult

object Entities {
  case class MetaVersion(x: Int, y: Int, z: Int)
  case class Meta(name: String, version: MetaVersion)

  case class HealthCheck(ok: Boolean, status: String)

  //    entities = [
  //    {
  //      name = "target"
  //      index = "targets"
  //      searchIndex = "search_target"
  //    },
  case class ElasticsearchIndices(target: String, disease: String, drug: String, search: Seq[String])
  case class ElasticsearchEntity(name: String, index: String, searchIndex: String)

  case class ElasticsearchSettings(host: String, port: Int,
                                   indices: ElasticsearchIndices,
                                   entities: Seq[ElasticsearchEntity])

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

    implicit val esEntities = Json.reads[Entities.ElasticsearchEntity]
    implicit val esIndices = Json.reads[Entities.ElasticsearchIndices]
    implicit val esSettingsImp = Json.reads[Entities.ElasticsearchSettings]

    implicit val targetsBodyImp = Json.format[Entities.TargetsBody]
    implicit val apiErrorMessageImp = Json.format[entities.APIErrorMessage]
  }
}
