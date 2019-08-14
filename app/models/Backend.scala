package models

import javax.inject.Inject
import models.Functions._
import models.Violations.{InputParameterCheckError, PaginationError}
import play.api.libs.json.Json
import play.api.{Configuration, Environment, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}
import models.Entities._
import models.Entities.JSONImplicits._

class Backend @Inject()(config: Configuration,
                        env: Environment) {
  val logger = Logger(this.getClass)

  val defaultESSettings =
    loadConfigurationObject[Entities.ElasticsearchSettings]("ot.elasticsearch", config)

  lazy val getMeta: Meta = Meta("Open Targets API Beta", MetaVersion(0,0,1))
}
