package models

import clickhouse.ClickHouseProfile
import esecuele.Column._
import esecuele.{Functions => F, Query => Q, _}
import models.db.{QAOTF, Queryable}
import models.entities.Associations.DBImplicits._
import models.entities.Configuration.{DatasourceSettings, LUTableSettings, OTSettings}
import models.entities.Harmonic._
import models.entities.Network.DBImplicits._
import models.entities._
import play.api.Logger
import slick.basic.DatabaseConfig
import slick.jdbc.{GetResult, SQLActionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ClickhouseRetriever(dbConfig: DatabaseConfig[ClickHouseProfile], config: OTSettings) {
  import dbConfig.profile.api._

  implicit private def toSQL(q: Q): SQLActionBuilder = sql"""#${q.rep}"""
  implicit private def toSQL(q: Queryable): SQLActionBuilder = sql"""#${q.query.rep}"""

  val db = dbConfig.db
  val logger = Logger(this.getClass)
  val chSettings = config.clickhouse
  val datasources = chSettings.harmonic.datasources.toVector
  val diseaseNetworks = chSettings.disease.networks.map(x => x.name -> x).toMap
  val targetNetworks = chSettings.target.networks.map(x => x.name -> x).toMap

  def getDatasourceSettings: Future[Vector[DatasourceSettings]] =
    Future.successful(datasources)

  def getTargetNetworkList: Future[Vector[LUTableSettings]] =
    Future.successful(chSettings.target.networks.toVector)

  def getDiseaseNetworkList: Future[Vector[LUTableSettings]] =
    Future.successful(chSettings.disease.networks.toVector)

  def getNodeNeighbours(netSetttings: LUTableSettings, id: String): Future[Option[NetworkNode]] = {
    val q = Network(netSetttings, id).as[NetworkNode]
    db.run(q.asTry) map {
      case Success(x) => x.headOption
      case Failure(exception) =>
        logger.error(exception.getMessage)
        None
    }
  }

  def getUniqList[A](of: Seq[String], from: String)(implicit rconv: GetResult[A]): Future[Vector[A]] = {
    getUniqList[A](of, Column(from))(rconv)
  }

  def getUniqList[A](of: Seq[String], from: Column)(implicit rconv: GetResult[A]): Future[Vector[A]] = {
    val s = Select(of.map(column))
    val f = From(from)
    val g = GroupBy(of.map(column))
    val l = Limit(0, 100000)
    val q = Q(s, f, g, l)

    logger.debug(s"get distinct $of from $from with query ${q.toString}")
    val qq = q.as[A]

    db.run(qq.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(s"An exception was thrown ${ex.getMessage}")
        Vector.empty
    }
  }

  def executeQuery[A](q: Queryable)(implicit rconv: GetResult[A]) = {
    logger.debug(s"execute query from eselecu Q ${q.query.toString}")
    val qq = q.as[A]

    db.run(qq.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(s"An exception was thrown ${ex.getMessage}")
        Vector.empty
    }
  }

  def getAssociationsOTF(tableName: String, AId: String, AIDs: Set[String], BIDs: Set[String],
                         BFilter: Option[String],
                         datasourceSettings: Seq[DatasourceSettings],
                         pagination: Pagination) = {
    val weights = datasourceSettings.map(s => (s.id, s.weight))
    val dontPropagate = datasourceSettings.withFilter(!_.propagate).map(_.id).toSet
    val aotfQ = QAOTF(
      tableName,
      AId,
      AIDs,
      BIDs,
      BFilter,
      None,
      weights,
      dontPropagate,
      pagination.offset, pagination.size).query.as[AssociationOTF]

    logger.debug(aotfQ.statements.mkString("\n"))

    db.run(aotfQ.asTry).map {
      case Success(v) => v
      case Failure(ex)  =>
        logger.error(ex.toString)
        logger.error("harmonic associations query failed " +
          s"with query: ${aotfQ.statements.mkString(" ")}")
        Vector.empty
    }
  }

  def getAssocationsByDisease(id: String, indirect: Boolean, filter: Option[String],
                              datasourceSettings: Seq[DatasourceSettings],
                              pagination: Pagination) = {
    val weights = F.array(datasourceSettings.map(s => F.tuple(literal(s.id), literal(s.weight)))).as(Some("v"))
    val weightsIds = F.tupleElement(weights,literal(1)).as(Some("datasource_id"))
    val weightsValues = F.tupleElement(weights,literal(2)).as(Some("weight"))

    val dbName = if (indirect) "ot.aotf_indirect_d" else "at.aotf_direct_d"
    val dsName = Column("datasource_id")
    val diseaseId = Column("disease_id")
    val targetId = Column("target_id")
    val targetSymbol = Column("target_symbol")
    val targetName = Column("target_name")
    val maxHS = literal(Harmonic.maxValue(maxVectorElementsDefault, pExponentDefault, 1.0))
      .as(Some("max_hs_score"))
    val weight = F.ifThenElse(F.equals(weightsValues.name, literal(0.0)), literal(1.0), weightsValues.name).as(Some("w"))
    val dss = F.groupArray(F.tuple(column("datasource_harmonic"), weight.name, column("datasource_id"))).as(Some("dss"))
    val dts = F.groupArray(F.tuple(column("datatype_harmonic"), weight.name, column("datatype_id"))).as(Some("dts"))
    val N = F.count(literal(1)).as(Some("n"))
    val score = F.divide(F.arraySum(Some("(a, b) -> (a.1 * a.2) / pow(b, 2)"), F.arrayReverseSort(None, dss.name), F.arrayEnumerate(N.name)), maxHS.name).as(Some("score"))

    val W = With(Seq(maxHS, weight, dss, dts, N, score))
    val S = Select(Seq(
      targetId.name,
      score.name,
      F.tupleElement(dss.name, literal(1)),
      F.tupleElement(dss.name, literal(3)),
      F.tupleElement(dts.name, literal(1)),
      F.tupleElement(dts.name, literal(3)),
    ))

    val filterQ = filter match {
      case Some(f) =>
        F.and(F.equals(diseaseId.name, literal(id)),
          F.or(F.ngramSearchCaseInsensitive(targetSymbol.name, literal(f)),
            F.ngramSearchCaseInsensitive(targetName.name, literal(f))))
      case None =>
        F.equals(diseaseId.name, literal(id))
    }

    val Fr = From(Column(dbName), Some("a"))
    val P = PreWhere(filterQ)
    val G = GroupBy(Seq(diseaseId.name, targetId.name))
    val O = OrderBy(Seq(score.name.desc))
    val L = Limit(pagination.offset, pagination.size)
    val AJ = ArrayJoin(weights.name)
    val S2 = Select(Seq(weightsIds, weightsValues))
    val S3 = Select(Seq(weights))
    val FrSel = FromSelect(S3)

    val innerQ = Q(S2, FrSel, AJ).toColumn(Some("w"))

    val q =
      sql"""
        |#${W.rep}
        |#${S.rep}
        |#${Fr.rep}
        |left outer join #${innerQ.rep}
        |using (#${dsName.rep})
        |#${P.rep}
        |#${G.rep}
        |#${O.rep}
        |#${L.rep}
        |""".stripMargin.as[Association]

    logger.debug(q.statements.mkString("\n"))

    db.run(q.asTry).map {
      case Success(v) => v
      case _ =>
        logger.error("An exception was thrown after quering harmonic associations fixing a disease " +
          s"with query: ${q.statements.mkString(" ")}")
        Vector.empty
    }

  }

  def getAssocationsByTarget(id: String, indirect: Boolean, filter: Option[String],
                              datasourceSettings: Seq[DatasourceSettings],
                              pagination: Pagination) = {
    val weights = F.array(datasourceSettings.map(s => F.tuple(literal(s.id), literal(s.weight)))).as(Some("v"))
    val weightsIds = F.tupleElement(weights.name,literal(1)).as(Some("datasource_id"))
    val weightsValues = F.tupleElement(weights.name,literal(2)).as(Some("weight"))

    val dbName = if (indirect) "ot.aotf_indirect_d" else "ot.aotf_direct_d"
    val dsName = Column("datasource_id")
    val diseaseId = Column("disease_id")
    val diseaseLabel = Column("disease_label")
    val targetId = Column("target_id")
    val maxHS = literal(Harmonic.maxValue(maxVectorElementsDefault, pExponentDefault, 1.0))
      .as(Some("max_hs_score"))
    val weight = F.ifThenElse(F.equals(weightsValues.name, literal(0.0)), literal(1.0), weightsValues.name).as(Some("w"))
    val dss = F.groupArray(F.tuple(column("datasource_harmonic"), weight.name, column("datasource_id"))).as(Some("dss"))
    val dts = F.groupArray(F.tuple(column("datatype_harmonic"), weight.name, column("datatype_id"))).as(Some("dts"))
    val N = F.count(literal(1)).as(Some("n"))
    val range = F.range(literal(1), F.plus(N.name, literal(1))).as(Some("idx_v"))
    val score = F.divide(F.arraySum(Some("(a, b) -> (a.1 * a.2) / pow(b, 2)"), F.arrayReverseSort(None, dss.name), range.name), maxHS.name).as(Some("score"))

    val W = With(Seq(maxHS, weight, dss, dts, N, range, score))
    val S = Select(Seq(
      diseaseId.name,
      score.name,
      F.tupleElement(dss.name, literal(1)),
      F.tupleElement(dss.name, literal(3)),
      F.tupleElement(dts.name, literal(1)),
      F.tupleElement(dts.name, literal(3)),
    ))

    val filterQ = filter match {
      case Some(f) =>
        F.and(F.equals(targetId.name, literal(id)), F.ngramSearchCaseInsensitive(diseaseLabel.name, literal(f)))
      case None =>
        F.equals(targetId.name, literal(id))
    }

    val Fr = From(Column(dbName), Some("a"))
    val P = PreWhere(filterQ)
    val G = GroupBy(Seq(targetId.name, diseaseId.name))
    val O = OrderBy(Seq(score.name.desc))
    val L = Limit(pagination.offset, pagination.size)
    val AJ = ArrayJoin(weights.name)
    val S2 = Select(Seq(weightsIds, weightsValues))
    val S3 = Select(Seq(weights))
    val FrSel = FromSelect(S3)

    val innerQ = Q(S2, FrSel, AJ).toColumn(Some("w"))

    val q =
      sql"""
           |#${W.rep}
           |#${S.rep}
           |#${Fr.rep}
           |left outer join #${innerQ.rep}
           |using (#${dsName.rep})
           |#${P.rep}
           |#${G.rep}
           |#${O.rep}
           |#${L.rep}
           |""".stripMargin.as[Association]

    logger.debug(q.statements.mkString("\n"))

    db.run(q.asTry).map {
      case Success(v) => v
      case _ =>
        logger.error("An exception was thrown after quering harmonic associations fixing a disease " +
          s"with query: ${q.statements.mkString(" ")}")
        Vector.empty
    }
  }

  /** compute all associations for a disease specified by its `id`
   * and the network expansion method by `expandedBy` field which has to
   * be one of the names you can find in the configuration file in the section
   * ot.clickhouse.disease.networks field name
   * */
//  def computeAssociationsDiseaseFixed(id: String, expandedBy: Option[LUTableSettings],
//                                      datasourceSettings: Seq[DatasourceSettings],
//                                      pagination: Pagination) = {
//
//    val neighboursQ = expandedBy
//      .map(lut => Network(lut, id).as[NetworkNode].headOption).getOrElse(DBIOAction.successful(None))
//
//    val harmonicQ = Harmonic(config.clickhouse.target.associations.key,
//      config.clickhouse.disease.associations.key, id,
//      config.clickhouse.disease.associations.name,
//      datasourceSettings,
//      expandedBy,
//      pagination)
//
//    val plainQ = harmonicQ.as[Association]
//
//    logger.debug(harmonicQ.toString)
//
//    if (pagination.hasValidRange()) {
//      db.run(plainQ.asTry zip neighboursQ.asTry).map {
//        case (Success(v), Success(w)) =>
//          Associations(expandedBy, w, datasourceSettings, v)
//        case _ =>
//          logger.error("An exception was thrown after quering harmonic and neighbours")
//          Associations(expandedBy, None, datasourceSettings, Vector.empty)
//      }
//    } else {
//      Future.failed(InputParameterCheckError(
//        Vector(PaginationError(pagination.size))))
//    }
//  }

  /** compute all associations for a disease specified by its `id`
   * and the network expansion method by `expandedBy` field which has to
   * be one of the names you can find in the configuration file in the section
   * ot.clickhouse.disease.networks field name
   * */
//  def computeAssociationsTargetFixed(id: String, expandedBy: Option[LUTableSettings],
//                                      datasourceSettings: Seq[DatasourceSettings],
//                                      pagination: Pagination) = {
//
//    val neighboursQ = expandedBy
//      .map(lut => Network(lut, id).as[NetworkNode].headOption).getOrElse(DBIOAction.successful(None))
//
//    val harmonicQ = Harmonic(config.clickhouse.disease.associations.key,
//      config.clickhouse.target.associations.key, id,
//      config.clickhouse.target.associations.name,
//      datasourceSettings,
//      expandedBy,
//      pagination).as[Association]
//
//    logger.debug(harmonicQ.statements.mkString("\n"))
//
//    db.run(harmonicQ.asTry zip neighboursQ.asTry).map {
//      case (Success(v), Success(w)) =>
//        Associations(expandedBy, w, datasourceSettings, v)
//      case _ =>
//        logger.error("An exception was thrown after quering harmonic and neighbours")
//        Associations(expandedBy, None, datasourceSettings, Vector.empty)
//    }
//  }
}
