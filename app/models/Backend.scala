package models

import com.sksamuel.elastic4s.ElasticDsl.{idsQuery, search}
import javax.inject.Inject
import models.Functions._
import models.Violations.{InputParameterCheckError, PaginationError}
import play.api.libs.json.Json
import play.api.{Configuration, Environment, Logger}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.playjson._
import com.sksamuel.elastic4s.requests.searches.SearchResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}
import models.entities._
import models.Entities._
import models.Entities.JSONImplicits._

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
}

class Backend @Inject()(config: Configuration,
                        env: Environment) {
  val logger = Logger(this.getClass)

  val defaultESSettings =
    loadConfigurationObject[Entities.ElasticsearchSettings]("ot.elasticsearch", config)

  val defaultMetaInfo =
    loadConfigurationObject[Entities.Meta]("ot.meta", config)

  /** return meta information loaded from ot.meta settings */
  lazy val getMeta: Meta = defaultMetaInfo

  def getStatus(isOk: Boolean) = isOk match {
    case true => HealthCheck(true, "All good!")
    case false => HealthCheck(false, "Hmm, something wrong is going on here!")
  }

  lazy val getESClient = ElasticClient(JavaClient(
    ElasticProperties(s"http://${defaultESSettings.host}:${defaultESSettings.port}")))

  lazy val esRetriever = new ElasticRetriever(getESClient)
  // we must import the dsl
  import com.sksamuel.elastic4s.ElasticDsl._

  def getTargets(ids: Seq[String]): Future[IndexedSeq[Target]] =
    esRetriever.getIds(defaultESSettings.indices.target, ids, Target.fromJsValue)

  def getDrugs(ids: Seq[String]): Future[IndexedSeq[Drug]] =
    esRetriever.getIds(defaultESSettings.indices.drug, ids, Drug.fromJsValue)
}
