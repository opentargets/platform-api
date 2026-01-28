package models.entities

import com.sksamuel.elastic4s.ElasticApi.valueCountAgg
import com.sksamuel.elastic4s.ElasticDsl.{boolQuery, existsQuery, not, rangeQuery, should}
import com.sksamuel.elastic4s.requests.searches.*
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import com.sksamuel.elastic4s.requests.searches.sort.*
import models.Helpers.fromJsValue
import models.{Backend, ElasticRetriever}
import models.entities.Configuration.{ElasticsearchSettings, OTSettings}
import models.gql.Objects.interactionImp
import models.Results
import utils.MetadataUtils.getIndexWithPrefixOrDefault
import play.api.Logging
import play.api.libs.json.*
import sangria.schema.{Field, ListType, LongType, ObjectType, fields}

import scala.concurrent.{ExecutionContext, Future}
import models.gql.TypeWithId

case class Interactions(count: Long, rows: IndexedSeq[Interaction], id: String = "")
    extends TypeWithId

object Interactions extends Logging {
  val empty: Interactions = Interactions(0L, IndexedSeq.empty)
  val interactions: ObjectType[Backend, Interactions] = ObjectType(
    "Interactions",
    "Molecular interactions reported between targets, with total count and rows",
    fields[Backend, Interactions](
      Field("count",
            LongType,
            description = Some("Total number of interaction entries available for the query"),
            resolve = o => o.value.count
      ),
      Field("rows",
            ListType(interactionImp),
            description = Some("List of molecular interaction entries"),
            resolve = o => o.value.rows
      )
    )
  )

  def listResources(implicit
      ec: ExecutionContext,
      esSettings: ElasticsearchSettings,
      esRetriever: ElasticRetriever,
      otSettings: OTSettings
  ): Future[Seq[InteractionResources]] = {

    val indexName = esSettings.entities
      .find(_.name == "interaction_evidence")
      .map(_.index)
      .getOrElse("interaction_evidence")

    val cbIndex = getIndexWithPrefixOrDefault(indexName)

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
