package models

import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.playjson._
import com.sksamuel.elastic4s.requests.common.Operator

import scala.concurrent.ExecutionContext.Implicits.global
import com.sksamuel.elastic4s.requests.searches.{MultisearchResponseItem, SearchResponse}
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer.{FieldValueFactorFunctionModifier, FunctionScoreQuery}
import com.sksamuel.elastic4s.requests.searches.queries.matches.MultiMatchQuery
import com.sksamuel.elastic4s.{ElasticClient, RequestFailure, RequestSuccess, Response}
import models.entities.Configuration.ElasticsearchEntity
import models.entities._
import models.entities.SearchResult.JSONImplicits._
import play.api.Logger
import play.api.libs.json.{JsArray, JsError, JsPath, JsSuccess, JsValue, Json}

import scala.concurrent.Future

class ElasticRetriever(client: ElasticClient) {
  val logger = Logger(this.getClass)
  import com.sksamuel.elastic4s.ElasticDsl._
  def getIds[A](esIndex: String, ids: Seq[String], buildF: JsValue => Option[A]): Future[IndexedSeq[A]] = {
    ids match {
      case Nil => Future.successful(IndexedSeq.empty)
      case _ =>
        val elems: Future[Response[SearchResponse]] = client.execute {
          val q = search(esIndex).query {
            idsQuery(ids)
          } limit(Configuration.batchSize)

          logger.debug(client.show(q))
          q
        }

        elems.map {
          case _: RequestFailure => IndexedSeq.empty
          case results: RequestSuccess[SearchResponse] =>
            // parse the full body response into JsValue
            // thus, we can apply Json Transformations from JSON Play
            val result = Json.parse(results.body.get)

            logger.debug(Json.prettyPrint(result))

            val hits = (result \ "hits" \ "hits").get.as[JsArray].value

            val mappedHits = hits
              .map(jObj => {
                buildF(jObj)
              }).withFilter(_.isDefined).map(_.get)

            mappedHits
        }
    }
  }

  def getSearchResultSet(entities: Seq[ElasticsearchEntity],
                         qString: String,
                         pagination: Pagination): Future[SearchResults] = {
    val limitClause = pagination.toES
    val esIndices = entities.map(_.searchIndex)

    val keywordQueryFn = multiMatchQuery(qString)
      .analyzer("token")
      .field("id.raw", 100D)
      .field("keywords.raw", 100D)
      .field("name.raw", 100D)
      .operator(Operator.AND)

    val stringQueryFn = functionScoreQuery(simpleStringQuery(qString)
      .analyzer("token")
      .minimumShouldMatch("0")
      .defaultOperator("AND")
      .field("name", 50D)
      .field("description", 25D)
      .field("prefixes", 10D)
      .field("terms", 5D)
      .field("ngrams"))
      .functions(fieldFactorScore("multiplier")
        .factor(1.0)
        .modifier(FieldValueFactorFunctionModifier.NONE))

    val aggFns = Seq(
      termsAgg("entities", "entity.raw")
        .size(1000)
        .subaggs(termsAgg("categories", "category.raw").size(1000)),
      cardinalityAgg("total", "id.raw")
    )

    val filterQueries = boolQuery.must() :: Nil
    val fnQueries = boolQuery.should(keywordQueryFn, stringQueryFn) :: Nil
    val mainQuery = boolQuery.must(fnQueries ::: filterQueries)

    if (qString.length > 0) {
      client.execute {
        val aggregations =
          search(esIndices) query (fnQueries.head) aggs(aggFns) size(0)
        logger.debug(client.show(aggregations))
        aggregations trackTotalHits(true)
      }.zip {
        client.execute {
          val mhits = multi(
            search(esIndices) query (mainQuery) start (limitClause._1) limit (limitClause._2) trackTotalHits(true)
          )
          // TODO remove it
          logger.debug(client.show(mhits))
          mhits
        }
      }.map {
        case (aggregations, hits) =>
          val aggsJ = Json.parse(aggregations.result.aggregationsAsString)
          val aggs = aggsJ.validateOpt[SearchResultAggs] match {
            case JsSuccess(value, _) => value
            case JsError(errors) =>
              logger.error(errors.mkString("", "\n", ""))
              None
          }

          val totals = hits.result.successes.foldLeft(0L)((B, op) => B + op.totalHits)
          val res = hits.result.successes.flatMap(_.to[SearchResult])
            .groupBy(_.entity)
            .mapValues(identity)
            .withDefaultValue(Seq.empty)

          SearchResults(totals,
            hits.result.to[SearchResult].headOption,
            res("target"),
            res("drug"),
            res("disease"),
            aggs)
      }
    } else {
      Future.successful(SearchResults.empty)
    }
  }
}
