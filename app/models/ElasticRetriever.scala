package models

import com.google.inject.Inject
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.api.QueryApi
import com.sksamuel.elastic4s.requests.common.Operator
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.requests.searches.queries.NestedQuery
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer._
import com.sksamuel.elastic4s.requests.searches.queries.matches.MultiMatchQueryBuilderType
import models.entities.Configuration.ElasticsearchEntity
import models.entities.SearchResults._
import models.entities.SearchFacetsResults._
import models.entities._
import models.Helpers.Base64Engine
import play.api.Logging
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import com.sksamuel.elastic4s.requests.searches.term.TermQuery
import com.sksamuel.elastic4s.handlers.index.Search
import views.html.index.f

case class ResolverField(fieldname: Option[String], matched_queries: Boolean = false)
object ResolverField {
  def apply(fieldname: String): ResolverField = ResolverField(Some(fieldname))
  def apply(matched_queries: Boolean): ResolverField = ResolverField(None, matched_queries)
}

class ElasticRetriever @Inject() (
    client: ElasticClient,
    hlFields: Seq[String],
    searchEntities: Seq[String]
) extends Logging
    with QueryApi
    with ElasticRetrieverQueryBuilders {

  val hlFieldSeq: Seq[HighlightField] = hlFields.map(HighlightField(_))

  import com.sksamuel.elastic4s.ElasticDsl._

  private def decodeSearchAfter(searchAfter: Option[String]): collection.Seq[Any] =
    searchAfter
      .map { sa =>
        val vv =
          Try(Json.parse(Base64Engine.decode(sa)))
            .map(_.asOpt[JsArray])
            .fold(
              ex => {
                logger.error(s"base64 encoded ${ex.toString}")
                None
              },
              identity
            )
        val sValues = vv.map(_.value).getOrElse(Seq.empty)

        val flattenedValues = sValues
          .map {
            case JsNumber(n) => n.toDouble
            case JsString(s) => s
            case otherJs =>
              logger.warn(s"base64 included some unexpected js values ${otherJs.toString}")
              null
          }
          .filter(_ != null)

        logger.trace(
          s"base64 $sa decoded and parsed into JsValue " +
            s"as ${Json.stringify(vv.getOrElse(JsNull))} and transformed into " +
            s"${flattenedValues.mkString("Seq(", ", ", ")")}"
        )

        flattenedValues
      }
      .getOrElse({
        logger.info("No results decoded from search, returning empty collection.")
        Seq.empty
      })

  private def encodeSearchAfter(jsArray: Option[JsValue]): Option[String] =
    jsArray.map(jsv => Base64Engine.encode(Json.stringify(jsv))).map(new String(_))

  /** This fn represents a query where each kv from the map is used in
    * a bool must. Based on the query asked by `getByIndexedQuery` and aggregation is applied
    */
  def getAggregationsByQuery[A](
      esIndex: String,
      boolQuery: BoolQuery,
      aggs: Iterable[AbstractAggregation] = Iterable.empty
  ): Future[JsValue] = {
    val q = search(esIndex)
      .bool {
        boolQuery
      }
      .start(0)
      .limit(0)
      .aggs(aggs)
      .trackTotalHits(true)

    // just log and execute the query
    val elems: Future[Response[SearchResponse]] = client.execute {
      logger.debug(s"Elasticsearch query to execute: ${client.show(q)}")
      q
    }

    elems.map {
      case _: RequestFailure                       => JsNull
      case results: RequestSuccess[SearchResponse] =>
        // parse the full body response into JsValue
        // thus, we can apply Json Transformations from JSON Play
        val result = Json.parse(results.body.get)

        logger.trace(Json.prettyPrint(result))
        val aggs = (result \ "aggregations").getOrElse(JsNull)
        aggs
    }
  }

  /** This fn represents a query where each kv from the map is used in
    * a bool must. Based on the query asked by `getByIndexedQuery` and aggregation is applied
    */
  def getByIndexedQueryMust[A, V](
      esIndex: String,
      kv: Map[String, V],
      pagination: Pagination,
      buildF: JsValue => Option[A],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty
  ): Future[(IndexedSeq[A], JsValue, Long)] = {
    // just log and execute the query
    val indexQuery: IndexQuery[V] = IndexQuery(esIndex = esIndex,
                                               kv = kv,
                                               pagination = pagination,
                                               aggs = aggs,
                                               excludedFields = excludedFields
    )
    val searchRequest: SearchRequest = IndexQueryMust(indexQuery)
    getByIndexedQuery(searchRequest, sortByField, buildF)
  }

  def getByIndexedQueryMustWithFilters[A, V](
      esIndex: String,
      kv: Map[String, V],
      filters: Seq[Query],
      pagination: Pagination,
      buildF: JsValue => Option[A],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty
  ): Future[(IndexedSeq[A], JsValue, Long)] = {
    // just log and execute the query
    val indexQuery: IndexQuery[V] = IndexQuery(esIndex = esIndex,
                                               kv = kv,
                                               filters = filters,
                                               pagination = pagination,
                                               aggs = aggs,
                                               excludedFields = excludedFields
    )
    val searchRequest: SearchRequest = IndexQueryMust(indexQuery)
    getByIndexedQuery(searchRequest, sortByField, buildF)
  }

  /** This fn represents a query where each kv from the map is used in
    * a bool 'must' (AND) with optional filters that are 'must' (AND).
    */
  def getByIndexedTermsMust[A, V](
      esIndex: String,
      kv: Map[String, V],
      pagination: Pagination,
      buildF: JsValue => Option[A],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty,
      filter: Seq[Query] = Seq.empty
  ): Future[(IndexedSeq[A], JsValue, Long)] = {
    // just log and execute the query
    val indexQuery: IndexQuery[V] = IndexQuery(esIndex = esIndex,
                                               kv = kv,
                                               filters = filter,
                                               pagination = pagination,
                                               aggs = aggs,
                                               excludedFields = excludedFields
    )
    val searchRequest: SearchRequest = IndexTermsMust(indexQuery)
    getByIndexedQuery(searchRequest, sortByField, buildF)
  }

  /** This fn represents a query where each kv from the map is used in
    * a bool 'should' (OR) with optional filters that are 'must' (AND).
    */
  def getByIndexedTermsShould[A, V](
      esIndex: String,
      kv: Map[String, V],
      pagination: Pagination,
      buildF: JsValue => Option[A],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty,
      filter: Seq[Query] = Seq.empty
  ): Future[(IndexedSeq[A], JsValue, Long)] = {
    // just log and execute the query
    val indexQuery: IndexQuery[V] = IndexQuery(esIndex = esIndex,
                                               kv = kv,
                                               filters = filter,
                                               pagination = pagination,
                                               aggs = aggs,
                                               excludedFields = excludedFields
    )
    val searchRequest: SearchRequest = IndexTermsShould(indexQuery)
    getByIndexedQuery(searchRequest, sortByField, buildF)
  }

  /** This fn represents a query where each kv from the map is used in
    * a bool 'should'. Based on the query asked by `getByIndexedQuery` and aggregation is applied
    */
  def getByIndexedQueryShould[A, V](
      esIndex: String,
      kv: Map[String, V],
      pagination: Pagination,
      buildF: JsValue => Option[A],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty
  ): Future[(IndexedSeq[A], JsValue, Long)] = {
    val indexQuery: IndexQuery[V] = IndexQuery(esIndex = esIndex,
                                               kv = kv,
                                               pagination = pagination,
                                               aggs = aggs,
                                               excludedFields = excludedFields
    )
    val searchRequest: SearchRequest =
      IndexQueryShould(indexQuery)
    // log and execute the query
    getByIndexedQuery(searchRequest, sortByField, buildF)
  }

  private def getByIndexedQuery[A](
      searchRequest: SearchRequest,
      sortByField: Option[sort.FieldSort] = None,
      buildF: JsValue => Option[A]
  ): Future[(IndexedSeq[A], JsValue, Long)] = {
    // log and execute the query
    val searchResponse: Future[Response[SearchResponse]] = executeQuery(searchRequest, sortByField)
    // convert results into A
    searchResponse.map {
      handleSearchResponse(_, searchRequest, buildF)
    }
  }

  private def getMultiByIndexedQuery[A](
      searchRequest: MultiSearchRequest,
      sortByField: Option[sort.FieldSort] = None,
      buildF: JsValue => Option[A],
      resolverField: Option[ResolverField]
  ): Future[IndexedSeq[(IndexedSeq[A], JsValue, Long, JsValue)]] = {
    // log and execute the query
    val searchResponse: Future[Response[MultiSearchResponse]] =
      executeMultiQuery(searchRequest, sortByField)
    // convert results into A
    searchResponse.map {
      handleMultiSearchResponse(_, searchRequest, buildF, resolverField)
    }
  }

  private def executeQuery(
      searchRequest: SearchRequest,
      sortByField: Option[sort.FieldSort]
  ): Future[Response[SearchResponse]] =
    client.execute {
      val sortedSearchRequest = sortByField match {
        case Some(s) => searchRequest.sortBy(s)
        case None    => searchRequest
      }

      logger.info(s"Elasticsearch query: ${client.show(sortedSearchRequest)}")
      sortedSearchRequest
    }

  private def executeMultiQuery(
      searchRequest: MultiSearchRequest,
      sortByField: Option[sort.FieldSort]
  ): Future[Response[MultiSearchResponse]] =
    client.execute {
      val sortedSearchRequest = sortByField match {
        case Some(s) =>
          val sortedSearches = searchRequest.searches.map { search =>
            search.sortBy(s)
          }
          searchRequest.copy(searches = sortedSearches)
        case None => searchRequest
      }
      logger.info(s"Elasticsearch query: ${client.show(sortedSearchRequest)}")
      sortedSearchRequest
    }

  private def handleSearchResponse[A](
      searchResponse: Response[SearchResponse],
      searchQuery: SearchRequest,
      buildF: JsValue => Option[A]
  ): (IndexedSeq[A], JsValue, Long) =
    searchResponse match {
      case rf: RequestFailure =>
        logger.debug(s"Request failure for query: $searchQuery")
        logger.error(s"Elasticsearch error: ${rf.error}")
        (IndexedSeq.empty, JsNull, 0)
      case results: RequestSuccess[SearchResponse] =>
        // parse the full body response into JsValue
        val result = Json.parse(results.body.get)
        logger.trace(Json.prettyPrint(result))

        val hits = (result \ "hits" \ "hits").get.as[JsArray].value
        val aggs = (result \ "aggregations").getOrElse(JsNull)
        val total = (result \ "hits" \ "total" \ "value").as[Long]

        val mappedHits = hits
          .map { jObj =>
            buildF(jObj)
          }
          .withFilter(_.isDefined)
          .map(_.get)
          .to(IndexedSeq)
        (mappedHits, aggs, total)
    }

  private def handleMultiSearchResponse[A](
      searchResponse: Response[MultiSearchResponse],
      searchQuery: MultiSearchRequest,
      buildF: JsValue => Option[A],
      resolverField: Option[ResolverField]
  ): IndexedSeq[(IndexedSeq[A], JsValue, Long, JsValue)] =
    searchResponse match {
      case rf: RequestFailure =>
        logger.debug(s"Request failure for query: $searchQuery")
        logger.error(s"Elasticsearch error: ${rf.error}")
        IndexedSeq.empty
      case results: RequestSuccess[MultiSearchResponse] =>
        val result = Json.parse(results.body.get)
        logger.trace(Json.prettyPrint(result))
        val responses = (result \ "responses").get.as[JsArray].value
        responses.map { response =>
          val hits = (response \ "hits" \ "hits").get.as[JsArray].value
          val aggs = (response \ "aggregations").getOrElse(JsNull)
          val total = (response \ "hits" \ "total" \ "value").as[Long]
          val rf = resolverField match {
            case Some(ResolverField(Some(r), false)) =>
              hits
                .map { jObj =>
                  (jObj \ "_source" \ r).as[JsValue]
                }
                .headOption
                .getOrElse(JsNull)
            case Some(ResolverField(None, true)) =>
              hits
                .map { jObj =>
                  (jObj \ "matched_queries").as[JsArray].value.headOption.getOrElse(JsNull)
                }
                .headOption
                .getOrElse(JsNull)
            case _ => JsNull
          }
          val mappedHits = hits
            .map { jObj =>
              buildF(jObj)
            }
            .withFilter(_.isDefined)
            .map(_.get)
            .to(IndexedSeq)
          (mappedHits, aggs, total, rf)
        }.toIndexedSeq
    }

  private def handleInnerSearchResponse[A](
      searchResponse: Response[SearchResponse],
      searchQuery: SearchRequest,
      buildF: JsValue => Option[A],
      innerHitsName: String,
      parentField: Option[String]
  ): (IndexedSeq[IndexedSeq[A]], JsValue, IndexedSeq[Long], IndexedSeq[JsValue]) =
    searchResponse match {
      case rf: RequestFailure =>
        logger.debug(s"Request failure for query: $searchQuery")
        logger.error(s"Elasticsearch error: ${rf.error}")
        (IndexedSeq.empty, JsNull, IndexedSeq.empty, IndexedSeq.empty)
      case results: RequestSuccess[SearchResponse] =>
        val result = Json.parse(results.body.get)
        val hits = (result \ "hits" \ "hits").get.as[JsArray].value
        val parentfields = parentField match {
          case Some(pf) =>
            hits
              .map { jObj =>
                (jObj \ "_source" \ pf).as[JsValue]
              }
              .to(IndexedSeq)
          case None => hits.map(jObj => JsNull).to(IndexedSeq)
        }
        val innerHits = hits.map { jObj =>
          (jObj \ "inner_hits" \ innerHitsName \ "hits" \ "hits").get.as[JsArray].value
        }
        val aggs = (result \ "aggregations").getOrElse(JsNull)
        val total = hits
          .map { jObj =>
            (jObj \ "inner_hits" \ innerHitsName \ "hits" \ "total" \ "value").as[Long]
          }
          .to(IndexedSeq)
        val mappedHits = innerHits
          .map { jObj =>
            jObj
              .map { innerJObj =>
                buildF(innerJObj)
              }
              .withFilter(_.isDefined)
              .map(_.get)
              .to(IndexedSeq)
          }
          .to(IndexedSeq)
        (mappedHits, aggs, total, parentfields)
    }

  def getMultiByIndexedTermsMust[V, A](
      indexQueries: Seq[IndexQuery[V]],
      buildF: JsValue => Option[A],
      sortByField: Option[sort.FieldSort] = None,
      resolverField: Option[ResolverField]
  ): Future[IndexedSeq[(IndexedSeq[A], JsValue, Long, JsValue)]] = {
    val searchRequest: MultiSearchRequest = MultiIndexTermsMust(indexQueries)
    getMultiByIndexedQuery(searchRequest, sortByField, buildF, resolverField)
  }

  def getMultiQ[A](
      indexQueries: Seq[IndexBoolQuery],
      buildF: JsValue => Option[A],
      sortByField: Option[sort.FieldSort] = None,
      resolverField: Option[ResolverField]
  ): Future[IndexedSeq[(IndexedSeq[A], JsValue, Long, JsValue)]] = {
    val searchRequest: MultiSearchRequest = multiBoolQueryBuilder(indexQueries)
    getMultiByIndexedQuery(searchRequest, sortByField, buildF, resolverField)
  }

  /* Provide a specific Bool Query*/
  def getQ[A](
      esIndex: String,
      boolQ: BoolQuery,
      pagination: Pagination,
      buildF: JsValue => Option[A],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty
  ): Future[(IndexedSeq[A], JsValue, Long)] = {
    val indexQuery = IndexBoolQuery(esIndex = esIndex,
                                    boolQuery = boolQ,
                                    pagination = pagination,
                                    aggs = aggs,
                                    excludedFields = excludedFields
    )
    val searchRequest: SearchRequest = BoolQueryBuilder(indexQuery)
    getByIndexedQuery(searchRequest, sortByField, buildF)
  }

  def getInnerQ[A](
      esIndex: String,
      boolQ: BoolQuery,
      pagination: Pagination,
      buildF: JsValue => Option[A],
      innerHitsName: String,
      parentField: Option[String],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty
  ): Future[(IndexedSeq[IndexedSeq[A]], JsValue, IndexedSeq[Long], IndexedSeq[JsValue])] = {
    val limitClause = pagination.toES
    val searchRequest: SearchRequest = search(esIndex)
      .bool(boolQ)
      .start(limitClause._1)
      .limit(limitClause._2)
      .aggs(aggs)
      .trackTotalHits(true)
      .sourceInclude(parentField.get)
    val searchResponse: Future[Response[SearchResponse]] = executeQuery(searchRequest, sortByField)
    searchResponse.map {
      handleInnerSearchResponse(_, searchRequest, buildF, innerHitsName, parentField)
    }
  }

  def getByMustWithSearch[A](
      esIndex: String,
      kv: Map[String, Seq[String]],
      pageSize: Int,
      buildF: JsValue => Option[A],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty,
      searchAfter: Option[String] = None
  ): Future[(IndexedSeq[A], Long, Option[String])] = {

    val mustTerms = kv.toSeq.map(p => termsQuery(p._1, p._2))

    val sa = decodeSearchAfter(searchAfter).toSeq
    val q = search(esIndex)
      .bool {
        must(mustTerms)
      }
      .size(pageSize)
      .aggs(aggs)
      .trackTotalHits(true)
      .sourceExclude(excludedFields)
      .searchAfter(sa)

    // just log and execute the query
    val elems: Future[Response[SearchResponse]] = client.execute {
      val qq = sortByField match {
        case Some(s) =>
          val tie = sort.FieldSort("id.keyword").asc()
          q.sortBy(s, tie)
        case None => q
      }

      logger.debug(s"Elasticsearch query to execute: ${client.show(qq)}")
      qq
    }

    elems.map {
      case _: RequestFailure                       => (IndexedSeq.empty, 0, None)
      case results: RequestSuccess[SearchResponse] =>
        // parse the full body response into JsValue
        // thus, we can apply Json Transformations from JSON Play
        val result = Json.parse(results.body.get)

        logger.trace(Json.prettyPrint(result))
        val hits = (result \ "hits" \ "hits").get.as[JsArray].value
        val totalHits = results.result.totalHits

        val mappedHits = hits
          .map { jObj =>
            buildF(jObj)
          }
          .withFilter(_.isDefined)
          .map(_.get)
          .to(IndexedSeq)

        val hasNext = !(hits.size < pageSize) && pageSize > 0

        val seAf =
          if (hasNext) {
            val jsa = (hits.last \ "sort").toOption
            encodeSearchAfter(jsa)
          } else
            None

        (mappedHits, totalHits, seAf)
    }
  }

  def getByFreeQuery[A](
      esIndex: String,
      queryString: String,
      kv: Map[String, String],
      pagination: Pagination,
      buildF: JsValue => Option[A],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty,
      searchAfter: Option[String] = None
  ): Future[(IndexedSeq[A], JsValue, Option[String])] = {
    val limitClause = pagination.toES

    val boolQ = boolQuery().should(
      simpleStringQuery(queryString)
        .defaultOperator("AND")
        .minimumShouldMatch("0"),
      multiMatchQuery(queryString)
        .matchType(MultiMatchQueryBuilderType.PHRASE_PREFIX)
        .prefixLength(1)
        .boost(100d)
        .fields("*")
    )

    val searchAfterEntries: Seq[Any] = decodeSearchAfter(searchAfter).toSeq

    val q =
      search(esIndex)
        .bool {
          must(boolQ)
            .filter(
              boolQuery().should(kv.toSeq.map(p => termQuery(p._1, p._2)))
            )
        }
        .start(limitClause._1)
        .limit(limitClause._2)
        .aggs(aggs)
        .trackTotalHits(true)
        .sourceExclude(excludedFields)
        .searchAfter(searchAfterEntries)

    val elems =
      client.execute {
        val qq = sortByField match {
          case Some(s) =>
            val tie = sort.FieldSort("_seq_no").asc()
            q.sortBy(s, tie)
          case None => q
        }

        logger.debug(s"Elasticsearch query to execute: ${client.show(qq)}")
        qq
      }

    elems.map {
      case _: RequestFailure                       => (IndexedSeq.empty, JsNull, None)
      case results: RequestSuccess[SearchResponse] =>
        // parse the full body response into JsValue
        // thus, we can apply Json Transformations from JSON Play
        val result = Json.parse(results.body.get)

        logger.trace(Json.prettyPrint(result))
        val hits = (result \ "hits" \ "hits").get.as[JsArray].value
        val aggs = (result \ "aggregations").getOrElse(JsNull)

        val mappedHits = hits
          .map { jObj =>
            buildF(jObj)
          }
          .withFilter(_.isDefined)
          .map(_.get)
          .to(IndexedSeq)

        val hasNext = !(hits.size < limitClause._2)

        val seAf =
          if (hasNext) {
            val jsa = (hits.last \ "sort").toOption
            encodeSearchAfter(jsa)
          } else
            None

        (mappedHits, aggs, seAf)
    }
  }

  def getByIds[A](
      esIndex: String,
      ids: Seq[String],
      buildF: JsValue => Option[A],
      excludedFields: Seq[String] = Seq.empty
  ): Future[IndexedSeq[A]] =
    ids match {
      case Nil =>
        logger.warn("No IDs provided to getByIds. Something is probably wrong.")
        Future.successful(IndexedSeq.empty)
      case _ =>
        val elems: Future[Response[SearchResponse]] = client.execute {
          val q = search(esIndex).query {
            idsQuery(ids)
          } limit (Configuration.batchSize) trackTotalHits (true) sourceExclude (excludedFields)

          logger.debug(s"Elasticsearch query to execute: ${client.show(q)}")
          q
        }

        elems.map {
          case _: RequestFailure                       => IndexedSeq.empty
          case results: RequestSuccess[SearchResponse] =>
            // parse the full body response into JsValue
            // thus, we can apply Json Transformations from JSON Play
            val result = Json.parse(results.body.get)

            logger.trace(Json.prettyPrint(result))

            val hits = (result \ "hits" \ "hits").get.as[JsArray].value

            val mappedHits = hits
              .map { jObj =>
                buildF(jObj)
              }
              .withFilter(_.isDefined)
              .map(_.get)

            mappedHits.to(IndexedSeq)
        }
    }

  def getTermsResultsMapping(
      entities: Seq[ElasticsearchEntity],
      queryTerms: Seq[String]
  ): Future[MappingResults] = {
    val queryTermsCleaned = queryTerms.filterNot(_.isEmpty)
    queryTermsCleaned match {
      case Nil =>
        logger.warn("No terms provided.")
        Future.successful(MappingResults.empty)
      case _ =>
        val esIndices = entities.withFilter(_.searchIndex.isDefined).map(_.searchIndex.get)
        val highlightOptions =
          HighlightOptions(highlighterType = Some("plain"), preTags = Seq(""), postTags = Seq(""))
        val highlightField = Seq(HighlightField("keywords.raw"))
        val mainQuery = termsQuery("keywords.raw", queryTermsCleaned)
        val aggFns = Seq(
          termsAgg("entities", "entity.raw")
            .size(2000)
            .subaggs(termsAgg("categories", "category.raw").size(2000)),
          cardinalityAgg("total", "id.raw")
        )
        val execAggsSearch = client
          .execute {
            val aggregations =
              search(searchEntities) query mainQuery aggs (aggFns) size (0)
            logger.trace(client.show(aggregations))
            aggregations trackTotalHits (true)
          }

        val execMainSearch = client.execute {
          val q = search(esIndices)
            .query(mainQuery)
            .start(0)
            .limit(10000)
            .highlighting(highlightOptions, highlightField)
            .trackTotalHits(true)
          q
        }
        val execSearch = execAggsSearch.zip(execMainSearch)

        execSearch
          .map { case (aggregations, hits) =>
            val aggsJ: JsValue = Json.parse(aggregations.result.aggregationsAsString)
            val aggs = aggsJ.validateOpt[SearchResultAggs] match {
              case JsSuccess(value, _) => value
              case JsError(errors) =>
                None
            }
            val results = {
              (Json.parse(hits.body.get) \ "hits" \ "hits").validate[Seq[SearchResult]] match {
                case JsSuccess(value, _) => value
                case JsError(errors) =>
                  Seq.empty
              }
            }
            val mappings: Seq[MappingResult] = queryTermsCleaned.map { term =>
              val termMappings =
                results.filter(_.highlights.contains(term.toLowerCase()))
              MappingResult(term, Some(termMappings))
            }
            MappingResults(mappings, aggs, hits.result.totalHits)
          }
    }
  }

  def getSearchFacetsResultSet(
      entities: Seq[ElasticsearchEntity],
      qString: String,
      pagination: Pagination,
      category: Option[String]
  ): Future[SearchFacetsResults] = {
    val limitClause = pagination.toES
    val esIndices = entities.withFilter(_.facetSearchIndex.isDefined).map(_.facetSearchIndex.get)
    val searchFields = Seq("label", "datasourceId")
    val hlFieldSeq = searchFields.map(f => HighlightField(f))

    val exactQueryFn = searchFields.map { f =>
      termQuery(f + ".keyword", qString).caseInsensitive(true).boost(10000d)
    }

    val fuzzyQueryFn = multiMatchQuery(qString)
      .fuzziness("AUTO")
      .prefixLength(1)
      .maxExpansions(50)
      .field("label", 100d)
      .field("datasourceId", 70d)
      .operator(Operator.OR)

    val categoryFilter = category match {
      case None | Some("") | Some("*") => matchAllQuery()
      case Some(categoryName)          => termQuery("category.keyword", categoryName)
    }

    val filterQueries = boolQuery().must(categoryFilter) :: Nil
    val fnQueries = {
      if (qString == "*") {
        matchAllQuery() :: Nil
      } else {
        boolQuery().should(Seq(fuzzyQueryFn) ++ exactQueryFn) :: Nil
      }
    }
    val mainQuery = boolQuery().must(fnQueries ::: filterQueries)
    val aggQuery = termsAgg("categories", "category.keyword").size(1000)

    val searchFacetCategories = client
      .execute {
        val aggregations = search(esIndices)
          .aggs(aggQuery)
          .trackTotalHits(true)
        logger.trace(client.show(aggregations))
        aggregations
      }
      .map { case (aggregations) =>
        val aggs = (Json.parse(aggregations.body.get) \ "aggregations" \ "categories" \ "buckets")
          .validate[Seq[SearchFacetsCategory]] match {
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            logger.error(errors.mkString("", " | ", ""))
            Seq.empty
        }
        aggs
      }
      .await

    if (qString.nonEmpty) {
      client
        .execute {
          val mhits = search(esIndices)
            .query(mainQuery)
            .start(limitClause._1)
            .limit(limitClause._2)
            .highlighting(HighlightOptions(highlighterType = Some("unified")), hlFieldSeq)
            .trackTotalHits(true)
          logger.trace(client.show(mhits))
          mhits
        }
        .map { case (hits) =>
          val jsHits = Json.parse(hits.body.get)
          logger.debug(Json.prettyPrint(jsHits))
          val sresults =
            (Json.parse(hits.body.get) \ "hits" \ "hits").validate[Seq[SearchFacetsResult]] match {
              case JsSuccess(value, _) => value
              case JsError(errors) =>
                logger.error(errors.mkString("", " | ", ""))
                Seq.empty
            }

          SearchFacetsResults(sresults, hits.result.totalHits, searchFacetCategories)
        }
    } else {
      Future.successful(SearchFacetsResults(Seq.empty, 0, searchFacetCategories))
    }
  }

  def getSearchResultSet(
      entities: Seq[ElasticsearchEntity],
      qString: String,
      pagination: Pagination
  ): Future[SearchResults] = {
    val limitClause = pagination.toES
    val esIndices = entities.withFilter(_.searchIndex.isDefined).map(_.searchIndex.get)

    val keywordQueryFn = multiMatchQuery(qString)
      .analyzer("token")
      .field("id.raw", 1000d)
      .field("keywords.raw", 1000d)
      .field("name.raw", 1000d)
      .operator(Operator.AND)

    val stringQueryFn = functionScoreQuery(
      simpleStringQuery(qString)
        .analyzer("token")
        .minimumShouldMatch("0")
        .defaultOperator("AND")
        .field("name", 50d)
        .field("description", 25d)
        .field("prefixes", 20d)
        .field("terms5", 15d)
        .field("terms25", 10d)
        .field("terms", 5d)
        .field("ngrams")
    ).functions(
      fieldFactorScore("multiplier")
        .factor(1.0)
        .modifier(FieldValueFactorFunctionModifier.NONE)
    )

    val aggFns = Seq(
      termsAgg("entities", "entity.raw")
        .size(1000)
        .subaggs(termsAgg("categories", "category.raw").size(1000)),
      cardinalityAgg("total", "id.raw")
    )

    val filterQueries = boolQuery().must() :: Nil
    val fnQueries = boolQuery().should(keywordQueryFn, stringQueryFn) :: Nil
    val mainQuery = boolQuery().must(fnQueries ::: filterQueries)

    if (qString.nonEmpty) {
      client
        .execute {
          val aggregations =
            search(searchEntities) query (fnQueries.head) aggs (aggFns) size (0)
          logger.trace(client.show(aggregations))
          aggregations trackTotalHits (true)
        }
        .zip {
          client.execute {
            val mhits = search(esIndices)
              .query(mainQuery)
              .start(limitClause._1)
              .limit(limitClause._2)
              .highlighting(HighlightOptions(highlighterType = Some("fvh")), hlFieldSeq)
              .trackTotalHits(true)
              .sourceExclude("terms", "terms5", "terms25")
            logger.trace(client.show(mhits))
            mhits
          }
        }
        .map { case (aggregations, hits) =>
          val aggsJ: JsValue = Json.parse(aggregations.result.aggregationsAsString)
          val aggs = aggsJ.validateOpt[SearchResultAggs] match {
            case JsSuccess(value, _) => value
            case JsError(errors) =>
              logger.error(errors.mkString("", " | ", ""))
              None
          }

          val jsHits = Json.parse(hits.body.get)
          logger.debug(Json.prettyPrint(jsHits))

          val sresults =
            (Json.parse(hits.body.get) \ "hits" \ "hits").validate[Seq[SearchResult]] match {
              case JsSuccess(value, _) => value
              case JsError(errors) =>
                logger.error(errors.mkString("", " | ", ""))
                Seq.empty
            }

          SearchResults(sresults, aggs, hits.result.totalHits)
        }
    } else {
      Future.successful(SearchResults.empty)
    }
  }
}

object ElasticRetriever extends Logging {

  /** *
    * SortBy case class use the `fieldName` to sort by and asc if `desc` is false
    * otherwise desc
    */
  def sortByAsc(fieldName: String): Some[FieldSort] = Some(sort.FieldSort(fieldName).asc())

  def sortByDesc(fieldName: String): Some[FieldSort] = Some(sort.FieldSort(fieldName).desc())

  def sortBy(fieldName: String, order: sort.SortOrder): Some[FieldSort] =
    Some(sort.FieldSort(field = fieldName, order = order))

}
