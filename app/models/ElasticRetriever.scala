package models

import play.api.Logging
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.playjson._
import com.sksamuel.elastic4s.requests.common.Operator

import scala.concurrent.ExecutionContext.Implicits.global
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer._
import com.sksamuel.elastic4s.requests.searches.queries.matches.{MultiMatchQuery, MultiMatchQueryBuilderType}
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.requests.searches.queries.{BoolQuery, NestedQuery}
import com.sksamuel.elastic4s.requests.searches.queries.term.TermQuery
import models.entities.Configuration.ElasticsearchEntity
import models.entities._
import models.entities.SearchResult.JSONImplicits._
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.Future

class ElasticRetriever(client: ElasticClient, hlFields: Seq[String],
                       searchEntities: Seq[String]) extends Logging {
  val hlFieldSeq = hlFields.map(HighlightField(_))

  import com.sksamuel.elastic4s.ElasticDsl._

  /** This fn represents a query where each kv from the map is used in
   * a bool must. Based on the query asked by `getByIndexedQuery` and aggregation is applied */
  def getAggregationsByQuery[A](esIndex: String, boolQuery: BoolQuery,
                                aggs: Iterable[AbstractAggregation] = Iterable.empty): Future[JsValue] = {
    val q = search(esIndex).bool {
      boolQuery
    }.start(0)
      .limit(0)
      .aggs(aggs)
      .trackTotalHits(true)

    // just log and execute the query
    val elems: Future[Response[SearchResponse]] = client.execute {
      logger.debug(client.show(q))
      q
    }

    elems.map {
      case _: RequestFailure => JsNull
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
  def getByIndexedQuery[A](esIndex: String, kv: Map[String, String],
                           pagination: Pagination,
                           buildF: JsValue => Option[A],
                           aggs: Iterable[AbstractAggregation] = Iterable.empty,
                           sortByField: Option[sort.FieldSort] = None,
                           excludedFields: Seq[String] = Seq.empty): Future[(IndexedSeq[A], JsValue)] = {
    val limitClause = pagination.toES
    val q = search(esIndex).bool {
      must(
        kv.toSeq.map(p => matchQuery(p._1, p._2))
      )
    }.start(limitClause._1)
      .limit(limitClause._2)
      .aggs(aggs)
      .trackTotalHits(true)
      .sourceExclude(excludedFields)

    // just log and execute the query
    val elems: Future[Response[SearchResponse]] = client.execute {
      val qq = sortByField match {
        case Some(s) => q.sortBy(s)
        case None => q
      }

      logger.debug(client.show(qq))
      qq
    }

    elems.map {
      case _: RequestFailure => (IndexedSeq.empty, JsNull)
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
          }).withFilter(_.isDefined).map(_.get)

        (mappedHits, aggs)
    }
  }

  def getByFreeQuery[A](esIndex: String, queryString: String,
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

    val q = search(esIndex).bool {
      must(boolQ)
        .filter(
          boolQuery().should(kv.toSeq.map(p => termQuery(p._1, p._2)))
        )
    }.start(limitClause._1)
      .limit(limitClause._2)
      .aggs(aggs)
      .trackTotalHits(true)
      .sourceExclude(excludedFields)
      .searchAfter(searchAfter)

    // just log and execute the query
    val elems: Future[Response[SearchResponse]] = client.execute {
      val qq = sortByField match {
        case Some(s) =>
          val tie = sort.FieldSort("_id").asc()
          q.sortBy(s, tie)
        case None => q
      }

      logger.debug(client.show(qq))
      qq
    }

    elems.map {
      case _: RequestFailure => (IndexedSeq.empty, JsNull, Nil)
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
          }).withFilter(_.isDefined).map(_.get)

        val hasNext = !(hits.size < limitClause._2)

        val seAf =
          if
          (hasNext) (hits.last \ "sort").get.as[Seq[String]]
          else
            Nil

        (mappedHits, aggs, seAf)
    }
  }

  def getByIds[A](esIndex: String, ids: Seq[String], buildF: JsValue => Option[A],
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
    val esIndices = entities.withFilter(_.searchIndex.isDefined).map(_.searchIndex.get)

    val keywordQueryFn = multiMatchQuery(qString)
      .analyzer("token")
      .field("id.raw", 1000D)
      .field("keywords.raw", 1000D)
      .field("name.raw", 1000D)
      .operator(Operator.AND)

    val stringQueryFn = functionScoreQuery(simpleStringQuery(qString)
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
      client.execute {
        val aggregations =
          search(searchEntities) query (fnQueries.head) aggs (aggFns) size (0)
        logger.debug(client.show(aggregations))
        aggregations trackTotalHits (true)
      }.zip {
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
      }.map {
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

          val sresults = (Json.parse(hits.body.get) \ "hits" \ "hits").validate[Seq[SearchResult]] match {
            case JsSuccess(value, _) => value
            case JsError(errors) =>
              logger.error(errors.mkString("", "\n", ""))
              Seq.empty
          }

          SearchResults(sresults,
            aggs, hits.result.totalHits)
      }
    } else {
      Future.successful(SearchResults.empty)
    }
  }
}

object ElasticRetriever extends Logging {

  def aggregationFilterProducer(settings: AggregationSettings, mappings: Map[String, AggregationMapping]) = {
    val filtersByName = settings.filters.groupBy(_.name).filterKeys(mappings.contains).map {
      case (facet, filters) =>
        val mappedFacet = mappings(facet)
        val ff = filters.foldLeft(BoolQuery()) {
          (b, filter) =>
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

    }.withDefaultValue(BoolQuery())

    val overallFilters = filtersByName.foldLeft(BoolQuery()) {
      case (b, f) => b.withMust(f._2)
    }

    val namesR = mappings.keys.toList.reverse
    if (namesR.size > 1) {
      val mappedMappgings = mappings.map(p => p._1 -> filtersByName(p._1))
        .toList.combinations(namesR.size - 1).toList

      val cartesianProd = (namesR zip mappedMappgings).toMap
        .mapValues(_.foldLeft(BoolQuery()) {
          (b, q) => b.withMust(q._2)
        })

      logger.debug(s"overall filters $overallFilters")
      cartesianProd foreach {
        el =>
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

  def sortBy(fieldName: String, order: sort.SortOrder) = Some(sort.FieldSort(field = fieldName, order = order))
}