package models

import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.playjson._

import scala.concurrent.ExecutionContext.Implicits.global
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer.{FieldValueFactorFunctionModifier, FunctionScoreQuery}
import com.sksamuel.elastic4s.requests.searches.queries.matches.MultiMatchQuery
import com.sksamuel.elastic4s.{ElasticClient, RequestFailure, RequestSuccess, Response}
import models.Functions.parsePaginationTokensForES
import models.entities._
import models.entities.SearchResult.JSONImplicits._
import play.api.libs.json.{JsArray, JsError, JsPath, JsSuccess, JsValue, Json}

import scala.concurrent.Future

class ElasticRetriever(client: ElasticClient) {
  import com.sksamuel.elastic4s.ElasticDsl._
  def getIds[A](esIndex: String, ids: Seq[String], buildF: JsValue => Option[A]): Future[IndexedSeq[A]] = {
    ids match {
      case Nil => Future.successful(IndexedSeq.empty)
      case _ =>
        val elems: Future[Response[SearchResponse]] = client.execute {
          search(esIndex).query {
            idsQuery(ids)
          }
        }

        elems.map {
          case _: RequestFailure => IndexedSeq.empty
          case results: RequestSuccess[SearchResponse] =>
            // parse the full body response into JsValue
            // thus, we can apply Json Transformations from JSON Play
            val result = Json.parse(results.body.get)
            val hits = (result \ "hits" \ "hits").get.as[JsArray].value

            val mappedHits = hits
              .map(jObj => {
                buildF(jObj)
              }).withFilter(_.isDefined).map(_.get)

            mappedHits
        }
    }
  }

  def getSearchResultSet(indices: Seq[String],
                         qString: String,
                         pageIndex: Option[Int],
                         pageSize: Option[Int]): Future[SearchResults] = {
    val limitClause = parsePaginationTokensForES(pageIndex, pageSize)

    val keywordQueryFn = functionScoreQuery(multiMatchQuery(qString)
      .analyzer("keyword")
      .field("id.keyword", 100D)
      .field("keywords.keyword", 100D)
      .field("name.keyword", 100D))
      .functions(fieldFactorScore("multiplier")
        .factor(1.0)
        .modifier(FieldValueFactorFunctionModifier.NONE))

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
      termsAgg("entities", "entity.keyword")
        .size(1000)
        .subaggs(termsAgg("categories", "category.keyword").size(1000)),
      cardinalityAgg("total", "id.keyword")
    )

    val filterQueries = boolQuery.must() :: Nil
    val fnQueries = boolQuery.should(keywordQueryFn, stringQueryFn) :: Nil
    val mainQuery = boolQuery.must(fnQueries ::: filterQueries)

    if (qString.length > 0) {
      client.execute {
        val aggregations =
          search(indices) query (fnQueries.head) aggs(aggFns) size(0)
        println(client.show(aggregations))
        aggregations trackTotalHits(true)
      }.zip {
        client.execute {
          val hits =
            search(indices) query (mainQuery) start (limitClause._1) limit (limitClause._2) trackTotalHits(true)

          println(client.show(hits))
          hits
        }
      }.map {
        case (aggregations, hits) =>
          val aggsJ = Json.parse(aggregations.result.aggregationsAsString)
          val aggs = aggsJ.validateOpt[SearchResultAggs] match {
            case JsSuccess(value, _) => value
            case JsError(errors) =>
              errors.foreach(println)
              None
          }

          SearchResults(hits.result.totalHits, hits.result.to[SearchResult], aggs)
      }
    } else {
      Future.successful(SearchResults(0, IndexedSeq.empty, None))
    }
  }
}
