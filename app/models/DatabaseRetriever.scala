package models

import clickhouse.ClickHouseProfile
import models.entities._
import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._
import slick.basic.DatabaseConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class DatabaseRetriever(dbConfig: DatabaseConfig[ClickHouseProfile]) {
  val db = dbConfig.db
  val logger = Logger(this.getClass)

  import dbConfig.profile.api._

  def getDatasources: Future[Vector[DataField]] = {
    val colNamePrefix = evidencesConfig.datasourceColumnNamePrefix
    val tableName = evidencesConfig.tableNameByDisease

    val plainQ =
      sql"""
           |select
           |  c.name as column_name,
           |  c.type as column_type
           |from system.columns c
           |where c.name like ${colNamePrefix} and c.table = $tableName
           |order by column_name
         """.stripMargin.as[DataField]

    //    plainQ.statements.foreach(println)

    db.run(plainQ.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(ex.getMessage)
        Vector.empty
    }
  }
}
