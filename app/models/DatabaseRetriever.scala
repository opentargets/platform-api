package models

import clickhouse.ClickHouseProfile
import models.entities.Configuration.{DatasourceSettings, LUTableSettings, OTSettings, TargetSettings}
import models.entities._
import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._
import slick.basic.DatabaseConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import esecuele._
import esecuele.{Query => Q}
import esecuele.Column._
import esecuele.{Functions => F}
import models.entities.Harmonic._
import models.entities.Associations._
import models.entities.Network.DBImplicits._
import models.entities.Associations.DBImplicits._
import models.entities.Violations.{InputParameterCheckError, PaginationError}
import sangria.validation.Violation
import slick.dbio.DBIOAction
import slick.jdbc.{GetResult, SQLActionBuilder}

class DatabaseRetriever(dbConfig: DatabaseConfig[ClickHouseProfile], config: OTSettings) {
  import dbConfig.profile.api._

  implicit private def toSQL(q: Q): SQLActionBuilder = sql"""#${q.rep}"""

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

  def getUniqList[A](of: Seq[String], from: String)(implicit rconv: GetResult[A]) = {
    val s = Select(of.map(column))
    val f = From(column(from))
    val g = GroupBy(of.map(column))
    val q = Q(s, f, g)

    logger.debug(s"get distinct $of from $from with query ${q.toString}")
    val qq = q.as[A]

    db.run(qq.asTry).map {
      case Success(v) => v
      case _ =>
        logger.error("An exception was thrown after quering harmonic and neighbours")
        Vector.empty
    }
  }

  def buildAOTFQuery(tableName: String, AId: String, AIDs: Set[String], BIDs: Set[String],
                     BFilter: Option[String], orderScoreBy: Option[(String, String)],
                     datasourceWeights: Seq[(String, Double)],
                     nonPropagatedDatasources: Set[String],
                     pagination: Pagination): Q = {

    val A = column("A")
    val B = column("B")
    val DS = column("datasource_id")
    val DT = column("datatype_id")
    val AData = column("A_data")
    val BData = column("B_data")
    val T = column(tableName)

    val WC = F.arrayJoin(F.array(datasourceWeights.map(s => F.tuple(literal(s._1), literal(s._2)))))
      .as(Some("weightPair"))
    val DSFieldWC = F.tupleElement(WC.name,literal(1)).as(Some("datasource_id"))
    val WFieldWC = F.toNullable(F.tupleElement(WC.name, literal(2))).as(Some("weight"))

    // transform weights vector into a table to extract each value of each tuple
    val q = Q(
      With(WC :: Nil),
      Select(DSFieldWC :: WFieldWC :: Nil),
      OrderBy(DSFieldWC.asc :: Nil)
    )

    val leftIdsC = F.arrayJoin(F.array(AIDs.map(literal).toSeq)).as(Some("AIDs"))
    val leftIdsQ = Q(With(leftIdsC :: Nil), Select(leftIdsC.name :: Nil))

    // build the boolean expression. Either with datasource propagation limitation (rna expression mainly)
    // or not then all simplifies quite a lot
    val nonPP = F.array(nonPropagatedDatasources.map(literal).toSeq)
    val expressionLeft = if (nonPropagatedDatasources.nonEmpty) {
      F.or(
        F.and(
          F.in(A, leftIdsQ.toColumn(None)),
          F.notIn(DS, nonPP)),
        F.and(
          F.in(DS, nonPP),
          F.equals(A, literal(AId))
        )
      )
    } else
      F.in(A, leftIdsQ.toColumn(None))

    // in the case we also want to filter B set
    val expressionLeftRight = if (BIDs.nonEmpty) {
      val rightIdsC = F.arrayJoin(F.array(BIDs.map(literal).toSeq)).as(Some("B_ids"))
      val rightIdsQ = Q(With(rightIdsC :: Nil), Select(rightIdsC.name :: Nil))
      F.and(
        expressionLeft,
        F.in(B, rightIdsQ.toColumn(None)),
      )
    } else {
      expressionLeft
    }

    val DSScore = F.arraySum(
      None,
      F.arrayMap(
        "(x, y) -> x / pow(y, 2)",
        F.arrayReverseSort(None,
          F.groupArray(column("row_score"))
        ),
        F.arrayEnumerate(F.groupArray(column("row_score")))
      )
    ).as(Some("score_datasource"))

    val DTAny = F.any(DT).as(Some(DT.rep))
    val DSW = F.ifNull(F.any(column("weight")), literal(1.0)).as(Some("datasource_weight"))

    val withDT = With(DSScore :: DTAny :: DSW :: Nil)
    val selectDTScores = Select(B :: DSW.name :: DTAny.name :: DS :: DSScore.name :: Nil)
    val fromT = From(T, Some("l"))
    val joinWeights = Join(q.toColumn(None), Some("LEFT"), Some("OUTER"), false, Some("r"), DS :: Nil)
    val preWhereQ = PreWhere(expressionLeftRight)
    val groupByQ = GroupBy(B :: DS :: Nil)
    val havingQ = BFilter match {
      case Some(matchStr) => Some(Having(F.like(BData.name, literal(s"%$matchStr%"))))
      case None => None
    }

    val aggByDatasourcesQ = Q(
      withDT,
      selectDTScores,
      fromT,
      Some(joinWeights),
      Some(preWhereQ),
      Some(groupByQ),
      havingQ
    )

    // final query to build the associations
    val maxHS = literal(Harmonic.maxValue(maxVectorElementsDefault, pExponentDefault, 1.0))
      .as(Some("max_hs_score"))

    val collectedDS = F.arrayReverseSort(Some("x -> x.2"), F.groupArray(
      F.tuple(
        F.divide(DSScore.name, maxHS.name),
        F.divide(F.multiply(DSScore.name, DSW.name), maxHS.name),
        DS,
        DT
      ))).as(Some("scores_vector"))


    val collectedDScored = F.arrayMap(s"(i, j) -> (i.1, (i.2) / pow(j, 2), i.3, i.4)",
        collectedDS.name,
        F.arrayEnumerate(collectedDS.name)
    ).as(Some("datasource_scores"))

    val scoreOverall = F.divide(F.arraySum(None,F.tupleElement(collectedDScored.name, literal(2))),
      maxHS.name).as(Some("score_overall"))

    val scoreDSs = F.arrayMap("x -> (x.3, x.1)",collectedDScored.name).as(Some("score_ds"))
    val scoreDTs = F.arrayMap("x -> (x.4, x.1)",collectedDScored.name).as(Some("score_dt"))
    val uniqDTs = F.groupUniqArray(DT)

    val mappedDTs = F.arrayMap(s"x -> (x, arrayReverseSort(arrayMap(b -> b.2, arrayFilter(a -> a.1 = x,${scoreDTs.name.rep}))))",
      uniqDTs.name).as(Some("mapped_dts"))
    val scoredDTs = F.arrayMap("x -> (x.1, arraySum((i, j) -> i / pow(j,2), x.2, arrayEnumerate(x.2)))",
      mappedDTs.name).as(Some("datatype_scores"))

    val withScores = With(
      Seq(maxHS,
        collectedDS,
        collectedDScored,
        scoreDSs,
        scoreDTs,
        uniqDTs,
        mappedDTs,
        scoredDTs,
        scoreOverall)
    )
    val selectScores = Select(B :: scoreOverall.name :: scoredDTs.name :: scoreDSs.name :: Nil) // :: scoreDTs.name :: collectedDScored :: Nil)
    val fromAgg = From(aggByDatasourcesQ.toColumn(None))
    val groupByB = GroupBy(B :: Nil)
    val orderBySome = orderScoreBy match {
      case Some(p) => OrderBy((
        if (p._2 == "desc") Column(p._1).desc
        else Column(p._1).asc) :: Nil
      )
      case None => OrderBy(scoreOverall.desc :: Nil)
    }

    val limitC = Limit(pagination.offset, pagination.size)

    val rootQ = Q(withScores, selectScores, fromAgg, groupByB, orderBySome, limitC)
    logger.debug(rootQ.toString)

    rootQ
  }

  def getAssociationsOTF(tableName: String, AId: String, AIDs: Set[String], BIDs: Set[String],
                         BFilter: Option[String],
                         datasourceSettings: Seq[DatasourceSettings],
                         pagination: Pagination) = {
    val weights = datasourceSettings.map(s => (s.id, s.weight))
    val aotfQ = buildAOTFQuery(
      tableName,
      AId,
      AIDs,
      BIDs,
      BFilter,
      None,
      weights,
      Set("expression_atlas"),
      pagination).as[AssociationOTF]

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
