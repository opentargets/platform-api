package models

import com.google.inject.Inject
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.Operator
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer._
import com.sksamuel.elastic4s.requests.searches.queries.matches.MultiMatchQueryBuilderType
import com.sksamuel.elastic4s.requests.searches.queries.term.TermQuery
import com.sksamuel.elastic4s.requests.searches.queries.{BoolQuery, NestedQuery}
import models.entities.Configuration.ElasticsearchEntity
import models.entities.SearchResults._
import models.entities._
import play.api.Logging
import play.api.cache.AsyncCacheApi
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class ElasticRetriever @Inject()(client: ElasticClient,
                                 hlFields: Seq[String],
                                 searchEntities: Seq[String],
                                 cache: AsyncCacheApi)
  extends Logging
    with QueryApi
    with ElasticRetrieverQueryBuilders {

  val hlFieldSeq = hlFields.map(HighlightField(_))

  import com.sksamuel.elastic4s.ElasticDsl._

  /** This fn represents a query where each kv from the map is used in
   * a bool must. Based on the query asked by `getByIndexedQuery` and aggregation is applied */
  def getAggregationsByQuery[A](
      esIndex: String,
      boolQuery: BoolQuery,
      aggs: Iterable[AbstractAggregation] = Iterable.empty): Future[JsValue] = {
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
      logger.debug(client.show(q))
      q
    }

    elems.map {
      case _: RequestFailure                       => JsNull
      case results: RequestSuccess[SearchResponse] =>
        // parse the full body response into JsValue
        // thus, we can apply Json Transformations from JSON Play
        val result = Json.parse(results.body.get)

        logger.debug(Json.prettyPrint(result))
        val hits = (result \ "hits" \ "hits").get.as[JsArray].value
        val aggs = (result \ "aggregations").getOrElse(JsNull)
        aggs
    }
  }

  /** This fn represents a query where each kv from the map is used in
    * a bool must. Based on the query asked by `getByIndexedQuery` and aggregation is applied */
  def getByIndexedQueryMust[A, V](
      esIndex: String,
      kv: Map[String, V],
      pagination: Pagination,
      buildF: JsValue => Option[A],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty): Future[(IndexedSeq[A], JsValue)] = {
    // just log and execute the query
    val searchRequest: SearchRequest = IndexQueryMust(esIndex, kv, pagination, aggs, excludedFields)
    getByIndexedQuery(searchRequest, sortByField, buildF)
  }

  /** This fn represents a query where each kv from the map is used in
    * a bool 'should'. Based on the query asked by `getByIndexedQuery` and aggregation is applied */
  def getByIndexedQueryShould[A, V](
      esIndex: String,
      kv: Map[String, V],
      pagination: Pagination,
      buildF: JsValue => Option[A],
      aggs: Iterable[AbstractAggregation] = Iterable.empty,
      sortByField: Option[sort.FieldSort] = None,
      excludedFields: Seq[String] = Seq.empty): Future[(IndexedSeq[A], JsValue)] = {
    val searchRequest: SearchRequest =
      IndexQueryShould(esIndex, kv, pagination, aggs, excludedFields)
    // log and execute the query
    getByIndexedQuery(searchRequest, sortByField, buildF)
  }

  private def getByIndexedQuery[A](
      searchRequest: SearchRequest,
      sortByField: Option[sort.FieldSort] = None,
      buildF: JsValue => Option[A]): Future[(IndexedSeq[A], JsValue)] = {
    // log and execute the query
    val searchResponse: Future[Response[SearchResponse]] = executeQuery(searchRequest, sortByField)
    // convert results into A
    searchResponse.map { handleSearchResponse(_, searchRequest, buildF) }
  }

  private def executeQuery(
      searchRequest: SearchRequest,
      sortByField: Option[sort.FieldSort]): Future[Response[SearchResponse]] = {
    client.execute {
      val sortedSearchRequest = sortByField match {
        case Some(s) => searchRequest.sortBy(s)
        case None    => searchRequest
      }

      logger.debug(s"Elasticsearch query: ${client.show(sortedSearchRequest)}")
      sortedSearchRequest
    }
  }

  private def handleSearchResponse[A](searchResponse: Response[SearchResponse],
                                      searchQuery: SearchRequest,
                                      buildF: JsValue => Option[A]): (IndexedSeq[A], JsValue) =
    searchResponse match {
      case rf: RequestFailure =>
        logger.debug(s"Request failure for query: $searchQuery")
        logger.error(s"Elasticsearch error: ${rf.error}")
        (IndexedSeq.empty, JsNull)
      case results: RequestSuccess[SearchResponse] =>
        // parse the full body response into JsValue
        val result = Json.parse(results.body.get)

        logger.debug(Json.prettyPrint(result))
        val hits = (result \ "hits" \ "hits").get.as[JsArray].value
        val aggs = (result \ "aggregations").getOrElse(JsNull)

        val mappedHits = hits
          .map(jObj => {
            buildF(jObj)
          })
          .withFilter(_.isDefined)
          .map(_.get)
        (mappedHits, aggs)
    }

  def getByMustWithSearch[A](esIndex: String,
                             kv: Map[String, Seq[String]],
                             pageSize: Int,
                             buildF: JsValue => Option[A],
                             aggs: Iterable[AbstractAggregation] = Iterable.empty,
                             sortByField: Option[sort.FieldSort] = None,
                             excludedFields: Seq[String] = Seq.empty,
                             searchAfter: Option[Seq[String]] = None)
    : Future[(IndexedSeq[A], Long, Option[Seq[String]])] = {

    val mustTerms = kv.toSeq.map(p => termsQuery(p._1, p._2))

    val q = search(esIndex)
      .bool {
        must(mustTerms)
      }
      .size(pageSize)
      .aggs(aggs)
      .trackTotalHits(true)
      .sourceExclude(excludedFields)
      .searchAfter(searchAfter.getOrElse(Nil))

    // just log and execute the query
    val elems: Future[Response[SearchResponse]] = client.execute {
      val qq = sortByField match {
        case Some(s) =>
          val tie = sort.FieldSort("id.keyword").asc()
          q.sortBy(s, tie)
        case None => q
      }

      logger.debug(client.show(qq))
      qq
    }

    elems.map {
      case _: RequestFailure                       => (IndexedSeq.empty, 0, None)
      case results: RequestSuccess[SearchResponse] =>
        // parse the full body response into JsValue
        // thus, we can apply Json Transformations from JSON Play
        val result = Json.parse(results.body.get)

        logger.debug(Json.prettyPrint(result))
        val hits = (result \ "hits" \ "hits").get.as[JsArray].value
        val totalHits = results.result.totalHits

        val mappedHits = hits
          .map(jObj => {
            buildF(jObj)
          })
          .withFilter(_.isDefined)
          .map(_.get)

        val hasNext = !(hits.size < pageSize) && pageSize > 0

        val seAf =
          if (hasNext) (hits.last \ "sort").get.asOpt[Seq[String]]
          else
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
      searchAfter: Seq[String] = Nil): Future[(IndexedSeq[A], JsValue, Seq[String])] = {
    val limitClause = pagination.toES

    val boolQ = boolQuery().should(
      simpleStringQuery(queryString)
        .defaultOperator("AND")
        .minimumShouldMatch("0"),
      multiMatchQuery(queryString)
        .matchType(MultiMatchQueryBuilderType.PHRASE_PREFIX)
        .prefixLength(1)
        .boost(100D)
        .fields("*")
    )
    val searchAfterEntries: Future[Seq[(Long, String)]] =
      Future.sequence(searchAfter
        .map(s => cache.get[(Long, String)](s))).map(s => s.flatten)

    val q: Future[SearchRequest] = searchAfterEntries.map { s =>
      logger.debug(s"Retrieved $s from cache.")
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
        .searchAfter(s match {
          case a if a.nonEmpty => Seq(a.head._1, a.head._2)
          case _ => Seq.empty
        })
    }

    // just log and execute the query
    val elems: Future[Response[SearchResponse]] = q.flatMap { query =>
      client.execute {
        val qq = sortByField match {
          case Some(s) =>
            val tie = sort.FieldSort("_id").asc()
            query.sortBy(s, tie)
          case None => query
        }

        logger.debug(client.show(qq))
        qq
      }
    }

    elems.map {
      case _: RequestFailure                       => (IndexedSeq.empty, JsNull, Nil)
      case results: RequestSuccess[SearchResponse] =>
        // parse the full body response into JsValue
        // thus, we can apply Json Transformations from JSON Play
        val result = Json.parse(results.body.get)

        logger.debug(Json.prettyPrint(result))
        val hits = (result \ "hits" \ "hits").get.as[JsArray].value
        val aggs = (result \ "aggregations").getOrElse(JsNull)

        val mappedHits = hits
          .map(jObj => {
            buildF(jObj)
          })
          .withFilter(_.isDefined)
          .map(_.get)

        val hasNext = !(hits.size < limitClause._2)

        val seAf: Option[String] =
          if (hasNext) {
            val arr: List[JsValue] = (hits.last \ "sort").get.asInstanceOf[JsArray].value.toList
            arr match {
              case a :: b :: Nil =>
                (a, b) match {
                  case (c: JsNumber, searchNextString: JsString) =>
                    val cacheEntry = (c.value.toLong, searchNextString.value)
                    cache.set(cacheEntry._2, cacheEntry, 1.hour)
                    Some(cacheEntry._2)
                  case missed =>
                    logger.warn(
                      "Search next requested but unable to parse last entry's sort parameter." +
                        s"Expected (JsNumber, JsString) but found ($missed")
                    None
                }
              case missed =>
                logger.warn(
                  s"Search next requested but unable to parse last entry's sort parameter. Received $missed")
                None

            }
          } else
            None

        (mappedHits, aggs, seAf.map(Seq(_)).getOrElse(Nil))
    }
  }

  def getByIds[A](esIndex: String,
                  ids: Seq[String],
                  buildF: JsValue => Option[A],
                  excludedFields: Seq[String] = Seq.empty): Future[IndexedSeq[A]] = {
    ids match {
      case Nil => Future.successful(IndexedSeq.empty)
      case _ =>
        val elems: Future[Response[SearchResponse]] = client.execute {
          val q = search(esIndex).query {
            idsQuery(ids)
          } limit (Configuration.batchSize) trackTotalHits (true) sourceExclude (excludedFields)

          logger.debug(client.show(q))
          q
        }

        elems.map {
          case _: RequestFailure                       => IndexedSeq.empty
          case results: RequestSuccess[SearchResponse] =>
            // parse the full body response into JsValue
            // thus, we can apply Json Transformations from JSON Play
            val result = Json.parse(results.body.get)

            logger.debug(Json.prettyPrint(result))

            val hits = (result \ "hits" \ "hits").get.as[JsArray].value

            val mappedHits = hits
              .map(jObj => {
                buildF(jObj)
              })
              .withFilter(_.isDefined)
              .map(_.get)

            mappedHits
        }
    }
  }

  def getSearchResultSet(entities: Seq[ElasticsearchEntity],
                         qString: String,
                         pagination: Pagination): Future[SearchResults] = {
    val limitClause = pagination.toES
    val esIndices = entities.withFilter(_.searchIndex.isDefined).map(_.searchIndex.get)

    val keywordQueryFn = multiMatchQuery(qString)
      .analyzer("token")
      .field("id.raw", 1000D)
      .field("keywords.raw", 1000D)
      .field("name.raw", 1000D)
      .operator(Operator.AND)

    val stringQueryFn = functionScoreQuery(
      simpleStringQuery(qString)
        .analyzer("token")
        .minimumShouldMatch("0")
        .defaultOperator("AND")
        .field("name", 50D)
        .field("description", 25D)
        .field("prefixes", 20D)
        .field("terms5", 15D)
        .field("terms25", 10D)
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
      client
        .execute {
          val aggregations =
            search(searchEntities) query (fnQueries.head) aggs (aggFns) size (0)
          logger.debug(client.show(aggregations))
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
            logger.debug(client.show(mhits))
            mhits
          }
        }
        .map {
          case (aggregations, hits) =>
            val aggsJ = Json.parse(aggregations.result.aggregationsAsString)
            val aggs = aggsJ.validateOpt[SearchResultAggs] match {
              case JsSuccess(value, _) => value
              case JsError(errors) =>
                logger.error(errors.mkString("", "\n", ""))
                None
            }

            if (logger.isDebugEnabled) {
              val jsHits = Json.parse(hits.body.get)
              logger.debug(Json.prettyPrint(jsHits))
            }

            val sresults =
              (Json.parse(hits.body.get) \ "hits" \ "hits").validate[Seq[SearchResult]] match {
                case JsSuccess(value, _) => value
                case JsError(errors) =>
                  logger.error(errors.mkString("", "\n", ""))
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

  /** aggregationFilterProducer returns a tuple where the first element is the overall list
    * of filters and the second is a map with the cartesian product of each aggregation with
    * the complementary list of filters
    */
  def aggregationFilterProducer(filters: Seq[AggregationFilter],
                                mappings: Map[String, AggregationMapping]) = {
    val filtersByName = filters
      .groupBy(_.name)
      .filterKeys(mappings.contains)
      .map {
        case (facet, filters) =>
          val mappedFacet = mappings(facet)
          val ff = filters.foldLeft(BoolQuery()) { (b, filter) =>
            val termKey = filter.path.zipWithIndex.last
            val termLevel = mappedFacet.pathKeys.lift
            val termPrefix = if (mappedFacet.nested) s"${mappedFacet.key}." else ""
            val keyName = termPrefix + s"${termLevel(termKey._2).getOrElse(mappedFacet.key)}.keyword"
            b.withShould(TermQuery(keyName, termKey._1))
          }

          if (mappedFacet.nested) {
            facet -> NestedQuery(mappedFacet.key, ff)
          } else {
            facet -> ff
          }

      }
      .withDefaultValue(BoolQuery())

    val overallFilters = filtersByName.foldLeft(BoolQuery()) {
      case (b, f) => b.withMust(f._2)
    }

    val namesR = mappings.keys.toList.reverse
    if (namesR.size > 1) {
      val mappedMappgings =
        mappings.map(p => p._1 -> filtersByName(p._1)).toList.combinations(namesR.size - 1).toList

      val cartesianProd = (namesR zip mappedMappgings).toMap
        .mapValues(_.foldLeft(BoolQuery()) { (b, q) =>
          b.withMust(q._2)
        })

      logger.debug(s"overall filters $overallFilters")
      cartesianProd foreach { el =>
        logger.debug(s"cartesian product ${el._1} -> ${el._2.toString}")
      }

      (overallFilters, cartesianProd)
    } else {
      (overallFilters, Map.empty[String, BoolQuery].withDefaultValue(BoolQuery()))
    }
  }

  /** *
    * SortBy case class use the `fieldName` to sort by and asc if `desc` is false
    * otherwise desc
    */
  def sortByAsc(fieldName: String) = Some(sort.FieldSort(fieldName).asc())

  def sortByDesc(fieldName: String) = Some(sort.FieldSort(fieldName).desc())

  def sortBy(fieldName: String, order: sort.SortOrder) =
    Some(sort.FieldSort(field = fieldName, order = order))

}
