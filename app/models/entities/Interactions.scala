package models.entities

import com.sksamuel.elastic4s.ElasticApi.valueCountAgg
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.sort._
import models.Helpers.fromJsValue
import models.{Backend, ElasticRetriever}
import models.entities.Configuration.ElasticsearchSettings
import models.entities.Interaction.interaction
import play.api.Logging
import play.api.libs.json._
import sangria.schema.{Field, ListType, LongType, ObjectType, OptionType, StringType, fields}

import scala.concurrent.{ExecutionContext, Future}

case class Interactions(count: Long, rows: IndexedSeq[JsValue])

object Interactions extends Logging {
  val interactions = ObjectType("Interactions",
    fields[Backend, Interactions](
      Field("count", LongType, description = None, resolve = o => o.value.count),
      Field("rows", ListType(interaction), description = None, resolve = o => o.value.rows)
    ))

  def find(id: String, dbName: Option[String], pagination: Option[Pagination])
                           (implicit ec: ExecutionContext, esSettings: ElasticsearchSettings, esRetriever: ElasticRetriever):
  Future[Option[Interactions]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val cbIndex = esSettings.entities
      .find(_.name == "interaction").map(_.index).getOrElse("interaction")

    val kv = List(
      Some("targetA.keyword" -> id),
      dbName.map("sourceDatabase.keyword" -> _)
    ).flatten.toMap

    val aggs = Seq(
      valueCountAgg("rowsCount", "targetA.keyword")
    )

    esRetriever.getByIndexedQuery(cbIndex, kv, pag, fromJsValue[JsValue], aggs,
      Some(sort.FieldSort("scoring", order = SortOrder.DESC))).map {
      case (Seq(), _) => None
      case (seq, agg) =>
        logger.debug(Json.prettyPrint(agg))

        val rowsCount = (agg \ "rowsCount" \ "value").as[Long]
        Some(Interactions(rowsCount, seq))
    }
  }

}


