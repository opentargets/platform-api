package models

import javax.inject.Inject
import models.Functions._
import play.api.{Configuration, Environment, Logger}
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.JavaClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}
import models.entities._
import models.Entities.JSONImplicits._
import models.Entities.{HealthCheck, Meta, Pagination}

class Backend @Inject()(config: Configuration,
                        env: Environment) {
  val logger = Logger(this.getClass)

  val defaultESSettings =
    loadConfigurationObject[Entities.ElasticsearchSettings]("ot.elasticsearch", config)

  val defaultMetaInfo =
    loadConfigurationObject[Entities.Meta]("ot.meta", config)

  /** return meta information loaded from ot.meta settings */
  lazy val getMeta: Meta = defaultMetaInfo

  def getStatus(isOk: Boolean): HealthCheck = isOk match {
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

  def search(qString: String, pagination: Option[Pagination] = Option(Pagination.mkDefault),
             indices: Seq[String] = defaultESSettings.indices.search): Future[SearchResults] =
    esRetriever.getSearchResultSet(indices, qString, pagination.map(_.index) , pagination.map(_.size))

  def msearch(qString: String, pagination: Option[Pagination] = Option(Pagination.mkDefault),
              entities: Seq[Entities.ElasticsearchEntity] = defaultESSettings.entities): Future[MSearchResults] =
    esRetriever.getMSearchResultSet(entities, qString, pagination.map(_.index) , pagination.map(_.size))
}
