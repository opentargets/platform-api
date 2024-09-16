package models.db

import esecuele._
import esecuele.{Functions => F}
import esecuele.Column._
import esecuele.{Query => Q}
import models.entities.Harmonic
import models.entities.Harmonic.pExponentDefault
import play.api.Logging

abstract class Queryable {
  def query: Query
}

/** QAOTF stands for Query for Associations on the fly computations
  *
  * @param tableName                table name to use for the associations on the fly query build
  * @param AId                      the left ID will be fixed and it is required. It is required as no
  *                                 propagation is still around
  * @param AIDs                     a set of optional left IDs to use as unioni with the single ID
  * @param BIDs                     an optional list of right IDs to fix to build the associations before the computations
  * @param BFilter                  before compute the numbers we restrict the right ids by text prefixing
  * @param orderScoreBy             Ordering, ist is a pair with name and mode of sorting ("score", "desc")
  * @param datasourceWeights        List of weights to use
  * @param nonPropagatedDatasources List of datasources to not propagate
  * @param offset                   where to start the chunk of rows to return
  * @param size                     how many rows to return in a chunk
  */
case class QAOTF(
    tableName: String,
    AId: String,
    AIDs: Set[String],
    BIDs: Set[String],
    BFilter: Option[String],
    orderScoreBy: Option[(String, String)],
    datasourceWeights: Seq[(String, Double)],
    nonPropagatedDatasources: Set[String],
    offset: Int,
    size: Int
) extends Queryable
    with Logging {
  val A: Column = column("A")
  val B: Column = column("B")
  val DS: Column = column("datasource_id")
  val DT: Column = column("datatype_id")
  val AData: Column = column("A_search")
  val BData: Column = column("B_search")
  val T: Column = column(tableName)
  val RowID: Column = column("row_id")
  val RowScore: Column = column("row_score")
  val maxHS: Column = literal(Harmonic.maxValue(100000, pExponentDefault, 1.0))
    .as(Some("max_hs_score"))

  val BFilterQ: Option[Column] = BFilter flatMap { case matchStr =>
    val tokens = matchStr
      .split(" ")
      .map { s =>
        F.like(BData.name, F.lower(literal(s"%${s.toLowerCase.trim}%")))
      }
      .toList

    tokens match {
      case h :: Nil         => Some(h)
      case h1 :: h2 :: rest => Some(F.and(h1, h2, rest*))
      case _                => None
    }
  }

  val DSScore: Column = F
    .arraySum(
      None,
      F.arrayMap(
        "(x, y) -> x / pow(y, 2)",
        F.arrayReverseSort(None, F.groupArray(RowScore)),
        F.arrayEnumerate(F.groupArray(RowScore))
      )
    )
    .as(Some("score_datasource"))

  val DSW: Column = F.ifNull(F.any(column("weight")), literal(1.0)).as(Some("datasource_weight"))

  val queryGroupByDS: Query = {
    val WC = F
      .arrayJoin(F.array(datasourceWeights.map(s => F.tuple(literal(s._1), literal(s._2)))))
      .as(Some("weightPair"))
    val DSFieldWC = F.tupleElement(WC.name, literal(1)).as(Some("datasource_id"))
    val WFieldWC = F.toNullable(F.tupleElement(WC.name, literal(2))).as(Some("weight"))

    // transform weights vector into a table to extract each value of each tuple
    val q = Q(
      With(WC :: Nil),
      Select(DSFieldWC :: WFieldWC :: Nil),
      OrderBy(DSFieldWC.asc :: Nil)
    )

    val leftIdsC = F.set((AIDs + AId).map(literal).toSeq)

    val nonPP = F.set(nonPropagatedDatasources.map(literal).toSeq)
    val expressionLeft = if (nonPropagatedDatasources.nonEmpty) {
      F.or(
        F.and(
          F.in(A, leftIdsC),
          F.notIn(DS, nonPP)
        ),
        F.equals(A, literal(AId))
      )
    } else
      F.in(A, leftIdsC)

    // in the case we also want to filter B set
    val expressionLeftRight = if (BIDs.nonEmpty) {
      val rightIdsC = F.set(BIDs.map(literal).toSeq)

      F.and(expressionLeft, F.in(B, rightIdsC))
    } else {
      expressionLeft
    }

    val expressionLeftRightWithBFilter =
      BFilterQ.map(f => F.and(f, expressionLeftRight)).getOrElse(expressionLeftRight)

    val DTAny = F.any(DT).as(Some(DT.rep))

    val withDT = With(DSScore :: DTAny :: DSW :: Nil)
    val selectDSScores = Select(B :: DSW.name :: DTAny.name :: DS :: DSScore.name :: Nil)
    val fromT = From(T, Some("l"))
    val joinWeights =
      Join(q.toColumn(None), Some("LEFT"), Some("OUTER"), false, Some("r"), DS :: Nil)
    val preWhereQ = PreWhere(expressionLeftRightWithBFilter)
    val groupByQ = GroupBy(B :: DS :: Nil)

    Q(
      withDT,
      selectDSScores,
      fromT,
      Some(joinWeights),
      Some(preWhereQ),
      Some(groupByQ)
    )
  }

  def simpleQuery(offset: Int, size: Int): Query = {
    val leftIdsC = F.set((AIDs + AId).map(literal).toSeq)

    val nonPP = F.set(nonPropagatedDatasources.map(literal).toSeq)
    val expressionLeft = if (nonPropagatedDatasources.nonEmpty) {
      F.or(
        F.and(
          F.in(A, leftIdsC),
          F.notIn(DS, nonPP)
        ),
        F.equals(A, literal(AId))
      )
    } else
      F.in(A, leftIdsC)

    // in the case we also want to filter B set
    val expressionLeftRight = if (BIDs.nonEmpty) {
      val rightIdsC = F.set(BIDs.map(literal).toSeq)
      F.and(
        expressionLeft,
        F.in(B, rightIdsC)
      )
    } else {
      expressionLeft
    }

    val expressionLeftRightWithBFilter =
      BFilterQ.map(f => F.and(f, expressionLeftRight)).getOrElse(expressionLeftRight)

    val DTAny = F.any(DT).as(Some(DT.rep))

    val withDT = With(DTAny :: Nil)
    val selectDSScores = Select(B :: DTAny.name :: DS :: Nil)
    val fromT = From(T, Some("l"))
    val preWhereQ = PreWhere(expressionLeftRightWithBFilter)
    val groupByQ = GroupBy(B :: DS :: Nil)

    val aggDSQ = Q(
      withDT,
      selectDSScores,
      fromT,
      Some(preWhereQ),
      Some(groupByQ)
    )

    val selectScores = Select(B :: Nil) // :: scoreDTs.name :: collectedDScored :: Nil)
    val fromAgg = From(aggDSQ.toColumn(None))
    val groupByB = GroupBy(B :: Nil)

    val limitC = Limit(offset, size)

    val rootQ = Q(selectScores, fromAgg, groupByB, limitC)
    logger.debug(rootQ.toString)

    rootQ
  }

  override val query: Query = {
    val collectedDS = F
      .arrayReverseSort(
        Some("x -> x.2"),
        F.groupArray(
          F.tuple(
            F.divide(DSScore.name, maxHS.name),
            F.divide(F.multiply(DSScore.name, DSW.name), maxHS.name),
            DS,
            DT
          )
        )
      )
      .as(Some("scores_vector"))

    val collectedDScored = F
      .arrayMap(
        s"(i, j) -> (i.1, (i.2) / pow(j, 2), i.3, i.4)",
        collectedDS.name,
        F.arrayEnumerate(collectedDS.name)
      )
      .as(Some("datasource_scores"))

    val scoreOverall = F
      .divide(F.arraySum(None, F.tupleElement(collectedDScored.name, literal(2))), maxHS.name)
      .as(Some("score"))

    val scoreDSs =
      F.arrayMap("x -> (x.3, x.1)", collectedDScored.name).as(Some("score_datasources"))
    val scoreDTs = F.arrayMap("x -> (x.4, x.1)", collectedDScored.name).as(Some("score_dt"))
    val uniqDTs = F.groupUniqArray(DT).as(Some("datatypes_v"))

    val mappedDTs = F
      .arrayMap(
        s"x -> (x, arrayReverseSort(arrayMap(b -> b.2, arrayFilter(a -> a.1 = x,${scoreDTs.name.rep}))))",
        uniqDTs.name
      )
      .as(Some("mapped_dts"))
    val scoredDTs = F
      .arrayMap(
        "x -> (x.1, arraySum((i, j) -> i / pow(j,2), x.2, arrayEnumerate(x.2)) / " +
          "arraySum(arrayMap((x, y) -> x / pow(y, 2),replicate(1.0, x.2),arrayEnumerate(x.2))) )",
        mappedDTs.name
      )
      .as(Some("score_datatypes"))

    val orderColumn = orderScoreBy.getOrElse((scoreOverall.name.rep, "desc"))
    val jointColumns = F.concat(scoredDTs.name, scoreDSs.name)
    val orderByC = F
      .ifThenElse(
        F.notEquals(
          F.indexOf(F.tupleElement(jointColumns.name, literal(1)), literal(orderColumn._1)),
          literal(0)
        ),
        F.tupleElement(
          F.arrayElement(
            jointColumns.name,
            F.indexOf(F.tupleElement(jointColumns.name, literal(1)), literal(orderColumn._1))
          ),
          literal(2)
        ),
        literal(0.0)
      )
      .as(Some("score_indexed"))

    val withScores = With(
      Seq(
        maxHS,
        collectedDS,
        collectedDScored,
        scoreDSs,
        scoreDTs,
        uniqDTs,
        mappedDTs,
        scoredDTs,
        scoreOverall,
        jointColumns,
        orderByC
      )
    )
    val selectScores = Select(
      B :: scoreOverall.name :: scoredDTs.name :: scoreDSs.name :: Nil
    ) // :: scoreDTs.name :: collectedDScored :: Nil)
    val fromAgg = From(queryGroupByDS.toColumn(None))
    val groupByB = GroupBy(B :: Nil)
    val orderBySome = orderColumn match {
      case ("score", order) =>
        OrderBy(
          (if (order == "desc") scoreOverall.name.desc
           else scoreOverall.name.asc) :: Nil
        )
      case (_, order) =>
        OrderBy(
          (if (order == "desc") orderByC.name.desc
           else orderByC.name.asc) :: Nil
        )
    }

    val limitC = Limit(offset, size)

    val rootQ = Q(withScores, selectScores, fromAgg, groupByB, orderBySome, limitC)
    logger.debug(rootQ.toString)

    rootQ
  }
}
