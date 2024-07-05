package models

import com.sksamuel.elastic4s.ElasticDsl.search
import com.sksamuel.elastic4s.api.QueryApi
import com.sksamuel.elastic4s.requests.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.SearchRequest
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import models.entities.Pagination
import play.api.Logging


/**
  * IndexQuery is a case class that represents a query to be executed on an Elasticsearch index.
  * @param esIndex the Elasticsearch index to query
  * @param kv a map of key-value pairs to form match queries with, where the key is the field name and the value is the match value
  * @param filters a sequence of additional filters to apply
  * @param pagination the pagination settings
  * @param aggs a sequence of aggregations to apply
  * @param excludedFields a sequence of fields to exclude from the results
  * @tparam V the type of the values in the key-value map
  */
case class IndexQuery[V](
    esIndex: String,
    kv: Map[String, V],
    filters: Seq[Query] = Seq.empty,
    pagination: Pagination,
    aggs: Iterable[AbstractAggregation] = Iterable.empty,
    excludedFields: Seq[String] = Seq.empty
)

trait ElasticRetrieverQueryBuilders extends QueryApi with Logging {

  def IndexQueryMust[V](indexQuery: IndexQuery[V]): SearchRequest =
    getByIndexQueryBuilder(indexQuery, must)

  def IndexQueryShould[V](
      indexQuery: IndexQuery[V]
  ): SearchRequest =
    getByIndexQueryBuilder(indexQuery, should)

  def getByIndexQueryBuilder[V](
      indexQuery: IndexQuery[V],
      f: Iterable[Query] => BoolQuery
  ): SearchRequest = {
    val limitClause = indexQuery.pagination.toES
    val query: Iterable[Query] = {
      val querySeq = indexQuery.kv.toSeq
      querySeq.flatMap { it =>
        it._2 match {
          case a: Iterable[Any] => a.map(iterVal => matchQuery(it._1, iterVal))
          case _                => Iterable(matchQuery(it._1, it._2))
        }
      }
    }
    val boolQuery: BoolQuery = f(query).filter(indexQuery.filters)
    search(indexQuery.esIndex)
      .bool(boolQuery)
      .start(limitClause._1)
      .limit(limitClause._2)
      .aggs(indexQuery.aggs)
      .trackTotalHits(true)
      .sourceExclude(indexQuery.excludedFields)
  }
}
