package models

import com.sksamuel.elastic4s.ElasticDsl.search
import com.sksamuel.elastic4s.requests.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.requests.searches.queries.{BoolQuery, Query}
import com.sksamuel.elastic4s.requests.searches.{QueryApi, SearchRequest}
import models.entities.Pagination
import play.api.Logging

trait ElasticRetrieverQueryBuilders extends QueryApi with Logging {

  def IndexQueryMust[A](
      esIndex: String,
      kv: Map[String, A],
      pagination: Pagination,
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      excludedFields: Seq[String] = Seq.empty
  ): SearchRequest = {
    getByIndexQueryBuilder(esIndex, kv, pagination, aggs, excludedFields, must)
  }

  def IndexQueryShould[A](
      esIndex: String,
      kv: Map[String, A],
      pagination: Pagination,
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      excludedFields: Seq[String] = Seq.empty
  ): SearchRequest = {
    getByIndexQueryBuilder(esIndex, kv, pagination, aggs, excludedFields, should)
  }

  def getByIndexQueryBuilder[A, V](
      esIndex: String,
      kv: Map[String, V],
      pagination: Pagination,
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      excludedFields: Seq[String] = Seq.empty,
      f: Iterable[Query] => BoolQuery
  ): SearchRequest = {
    val limitClause = pagination.toES
    val query: Iterable[Query] = {
      val querySeq = kv.toSeq
      querySeq.flatMap { it =>
        it._2 match {
          case a: Iterable[Any] => a.map(iterVal => matchQuery(it._1, iterVal))
          case _                => Iterable(matchQuery(it._1, it._2))
        }
      }
    }
    search(esIndex)
      .bool {
        f(
          query
        )
      }
      .start(limitClause._1)
      .limit(limitClause._2)
      .aggs(aggs)
      .trackTotalHits(true)
      .sourceExclude(excludedFields)
  }
}
