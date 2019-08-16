package models

import javax.inject.Inject
import models.Functions._
import models.Violations.{InputParameterCheckError, PaginationError}
import play.api.libs.json.Json
import play.api.{Configuration, Environment, Logger}
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

  lazy val getESClient = ElasticClient(JavaClient(ElasticProperties(s"http://${defaultESSettings.host}:${defaultESSettings.port}")))

  // we must import the dsl
  import com.sksamuel.elastic4s.ElasticDsl._

  def getTargets(ids: Seq[String]): Future[Seq[Target]] = {
    ids match {
      case Nil => Future.successful(Seq.empty)
      case _ =>
        val targets = getESClient.execute {
          search("19.09.b1_gene-data").query {
            idsQuery(ids)
          }
        }
//        val targets = ids.map(Target(_, Some("P001"), "BRAF", "B-Raf proto-oncogene, serine/threonine kinase",
//          Some("Protein kinase involved in the transduction of mitogenic signals from the cell membrane " +
//            "to the nucleus. May play a role in the postsynaptic responses of hippocampal neuron. " +
//            "Phosphorylates MAP2K1, and thereby contributes to the MAP kinase signal transduction pathway.")))

        targets.map {
          case _: RequestFailure => Seq.empty
          case results: RequestSuccess[SearchResponse] => results.result.to[Target]
        }
    }
  }
}
