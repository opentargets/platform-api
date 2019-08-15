package models

import javax.inject.Inject
import models.Functions._
import models.Violations.{InputParameterCheckError, PaginationError}
import play.api.libs.json.Json
import play.api.{Configuration, Environment, Logger}

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

  def getTargets(ids: Seq[String]): Future[Seq[Target]] = {
    ids match {
      case Nil => Future.successful(Seq.empty)
      case _ =>
        val targets = ids.map(Target(_, Some("P001"), "BRAF", "B-Raf proto-oncogene, serine/threonine kinase",
          Some("Protein kinase involved in the transduction of mitogenic signals from the cell membrane " +
            "to the nucleus. May play a role in the postsynaptic responses of hippocampal neuron. " +
            "Phosphorylates MAP2K1, and thereby contributes to the MAP kinase signal transduction pathway.")))

        Future.successful(targets)
    }
  }
}
