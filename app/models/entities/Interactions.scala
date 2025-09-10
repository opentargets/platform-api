package models.entities

import com.sksamuel.elastic4s.ElasticApi.valueCountAgg
import com.sksamuel.elastic4s.ElasticDsl.{boolQuery, existsQuery, not, rangeQuery, should}
import com.sksamuel.elastic4s.requests.searches.*
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import com.sksamuel.elastic4s.requests.searches.sort.*
import models.Helpers.fromJsValue
import models.{Backend, ElasticRetriever}
import models.entities.Configuration.ElasticsearchSettings
import models.gql.Objects.interactionImp
import models.Results
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.*
import sangria.schema.{Field, ListType, LongType, ObjectType, fields}

import scala.concurrent.{ExecutionContext, Future}

case class Interactions(count: Long, rows: IndexedSeq[Interaction])

object Interactions {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val interactions: ObjectType[Backend, Interactions] = ObjectType(
    "Interactions",
    fields[Backend, Interactions](
      Field("count", LongType, description = None, resolve = o => o.value.count),
      Field("rows", ListType(interactionImp), description = None, resolve = o => o.value.rows)
    )
  )

  def find(id: String,
           scoreThreshold: Option[Double],
           dbName: Option[String],
           pagination: Option[Pagination]
  )(implicit
      ec: ExecutionContext,
      esSettings: ElasticsearchSettings,
      esRetriever: ElasticRetriever
  ): Future[Option[Interactions]] = {

    val pag = pagination.getOrElse(Pagination.mkDefault)

    val cbIndex = esSettings.entities
      .find(_.name == "interaction")
      .map(_.index)
      .getOrElse("interaction")

    val kv = List(
      Some("targetA.keyword" -> id),
      dbName.map("sourceDatabase.keyword" -> _)
    ).flatten.toMap

    val filters = Seq(
      should(
        rangeQuery("scoring").gte(scoreThreshold.getOrElse(0.0d)),
        not(existsQuery("scoring"))
      )
    )

    val aggs = Seq(
      valueCountAgg("rowsCount", "targetA.keyword")
    )

    esRetriever
      .getByIndexedQueryMustWithFilters(
        cbIndex,
        kv,
        filters,
        pag,
        fromJsValue[Interaction],
        aggs,
        Some(sort.FieldSort("scoring", order = SortOrder.DESC))
      )
      .map {
        case Results(Seq(), _, _, _) => None
        case Results(seq, agg, _, _) =>
          logger.debug(Json.prettyPrint(agg))

          val rowsCount = (agg \ "rowsCount" \ "value").as[Long]
          Some(Interactions(rowsCount, seq))
      }
  }

  def listResources(implicit
      ec: ExecutionContext,
      esSettings: ElasticsearchSettings,
      esRetriever: ElasticRetriever
  ): Future[Seq[InteractionResources]] = {

    val cbIndex = esSettings.entities
      .find(_.name == "interaction_evidence")
      .map(_.index)
      .getOrElse("interaction_evidence")

    val queryAggs = Seq(
      TermsAggregation(
        "aggs",
        Some("interactionResources.sourceDatabase.keyword"),
        size = Some(100),
        subaggs = Seq(
          TermsAggregation(
            "aggs",
            Some("interactionResources.databaseVersion.keyword"),
            size = Some(100)
          )
        )
      )
    )

    val esQ = esRetriever.getAggregationsByQuery(cbIndex, boolQuery(), queryAggs) map {
      case obj: JsObject =>
        logger.debug(Json.prettyPrint(obj))

        val keys = ((obj \ "aggs" \ "buckets")
          .as[Seq[JsValue]])
          .map { el =>
            val k = (el \ "key").as[String]
            val v = (el \ "aggs" \ "buckets" \\ "key").take(1).head.as[String]
            InteractionResources(k, v)
          }

        keys
      case _ => Seq.empty
    }

    esQ
  }
}
