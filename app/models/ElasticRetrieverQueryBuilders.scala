package models

import com.sksamuel.elastic4s.ElasticDsl.search
import com.sksamuel.elastic4s.requests.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.requests.searches.queries.{BoolQuery, Query}
import com.sksamuel.elastic4s.requests.searches.{QueryApi, SearchRequest}
import models.entities.Pagination
import play.api.Logging

trait ElasticRetrieverQueryBuilders extends QueryApi with Logging {

  def IndexQueryMust(
      esIndex: String,
      kv: Map[String, String],
      pagination: Pagination,
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      excludedFields: Seq[String] = Seq.empty,
  ): SearchRequest = {
    getByIndexQueryBuilder(esIndex, kv, pagination, aggs, excludedFields, must)
  }

  def IndexQueryShould(
      esIndex: String,
      kv: Map[String, String],
      pagination: Pagination,
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      excludedFields: Seq[String] = Seq.empty,
  ): SearchRequest = {
    getByIndexQueryBuilder(esIndex, kv, pagination, aggs, excludedFields, should)
  }

  def getByIndexQueryBuilder[A](esIndex: String,
                                kv: Map[String, String],
                                pagination: Pagination,
                                aggs: Iterable[AbstractAggregation] = Iterable.empty,
                                excludedFields: Seq[String] = Seq.empty,
                                f: Iterable[Query] => BoolQuery): SearchRequest = {
    val limitClause = pagination.toES
    search(esIndex)
      .bool {
        f(
          kv.toSeq.map(p => matchQuery(p._1, p._2))
        )
      }
      .start(limitClause._1)
      .limit(limitClause._2)
      .aggs(aggs)
      .trackTotalHits(true)
      .sourceExclude(excludedFields)
  }
}
