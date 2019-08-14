package models

import clickhouse.ClickHouseProfile
import javax.inject.Inject
import models.Functions._
import models.Violations.{InputParameterCheckError, PaginationError}
import play.api.libs.json.Json
import play.api.{Configuration, Environment, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}

class Backend @Inject()(config: Configuration,
                        env: Environment) {
  val logger = Logger(this.getClass)
}
