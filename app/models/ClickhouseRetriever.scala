package models

import clickhouse.ClickHouseProfile
import esecuele.Column.*
import esecuele.{Query as Q, *}
import models.entities.Configuration.OTSettings
import models.entities.*
import net.logstash.logback.argument.StructuredArguments.keyValue
import services.ApplicationStart
import slick.basic.DatabaseConfig
import slick.jdbc.{GetResult, PositionedResult, SQLActionBuilder}
import utils.OTLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success}

class ClickhouseRetriever(config: OTSettings)(implicit
    val dbConfig: DatabaseConfig[ClickHouseProfile],
    val appStart: ApplicationStart
) extends OTLogging {

  import dbConfig.profile.api._

  val db_name = "clickhouse"

  implicit private def toSQL(q: Q): SQLActionBuilder = sql"""#${q.rep}"""

  var db = dbConfig.db
  val chSettings = config.clickhouse

  def getUniqList[A](of: Seq[String], from: String)(implicit
      rconv: GetResult[A]
  ): Future[Vector[A]] =
    getUniqList[A](of, Column(from))(rconv)

  def getUniqList[A](of: Seq[String], from: Column)(implicit
      rconv: GetResult[A]
  ): Future[Vector[A]] = {
    val s = Select(of.map(column))
    val f = From(from)
    val g = GroupBy(of.map(column))
    val l = Limit(0, 100000)
    val q = Q(s, f, g, l)

    logger.debug(s"getUniqList get distinct with query ${q.toString} for column ${of}",
                 keyValue("table", from)
    )
    val qq = q.as[A]

    appStart.DatabaseCallCounter.labelValues(db_name, "getUniqList").inc()

    db.run(qq.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(s"getUniqList an exception was thrown ${ex.getMessage}", ex)
        Vector.empty
    }
  }

  // clickhouse-jdbc's PreparedStatement (what Slick's sql"..." normally executes through) ignores
  // the trailing FORMAT JSONEachRow clause and returns typed per-column results instead of the
  // single JSON-blob column DbJsonParser/GetResult expect; a plain Statement respects it. So this
  // runs the raw statement directly instead of going through Slick's default PreparedStatement path.
  def executeQuery[A, B <: Q](q: B)(implicit rconv: GetResult[A]): Future[Vector[A]] = {
    logger.debug(s"execute query from esecuele Q ${q.toString}")
    val qStr = q.rep

    appStart.DatabaseCallCounter.labelValues(db_name, "executeQuery").inc()

    val action = SimpleDBIO[Vector[A]] { ctx =>
      val st = ctx.connection.createStatement()
      try {
        val rs = st.executeQuery(qStr)
        try {
          val pr = new PositionedResult(rs) { def close(): Unit = () }
          val b = Vector.newBuilder[A]
          while (pr.nextRow) b += rconv(pr)
          b.result()
        } finally rs.close()
      } finally st.close()
    }

    db.run(action.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(s"executeQuery an exception was thrown ${ex.getCause()} with Query $qStr", ex)
        Vector.empty // TODO: maybe we should return the error instead of an empty vector, to inform the user
    }
  }
}
