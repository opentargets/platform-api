package models.db

import esecuele._
import esecuele.{Functions => F}
import esecuele.Column._
import esecuele.{Query => Q}
import models.entities.Harmonic
import models.entities.Harmonic.{maxVectorElementsDefault, pExponentDefault}
import play.api.Logger

abstract class Queryable {
  def query: Query
}

/**
 * QAOTF stands for Query for Associations on the fly computations
 * TODO sorting by scores
 * TODO using the AIDs and BIDs to the facets
 */
case class QAOTF(tableName: String, AId: String, AIDs: Set[String], BIDs: Set[String],
                 BFilter: Option[String], orderScoreBy: Option[(String, String)],
                 datasourceWeights: Seq[(String, Double)],
                 nonPropagatedDatasources: Set[String],
                 offset: Int, size: Int) extends Queryable {
  val logger = Logger(this.getClass)

  val A = column("A")
  val B = column("B")
  val DS = column("datasource_id")
  val DT = column("datatype_id")
  val AData = column("A_search")
  val BData = column("B_search")
  val T = column(tableName)
  val RowID = column("row_id")
  val RowScore = column("row_score")
  val maxHS = literal(Harmonic.maxValue(100000, pExponentDefault, 1.0))
    .as(Some("max_hs_score"))

  val BFilterQ = BFilter flatMap  {
    case matchStr =>
      val tokens = matchStr.split(" ").map(s => {
        F.like(BData.name, F.lower(literal(s"%${s.toLowerCase.trim}%")))
      }).toList

      tokens match {
        case h :: Nil => Some(h)
        case h1 :: h2 :: rest => Some(F.and(h1, h2, rest:_*))
        case _ => None
      }
  }

  val DSScore = F.arraySum(
    None,
    F.arrayMap(
      "(x, y) -> x / pow(y, 2)",
      F.arrayReverseSort(None,
        F.groupArray(RowScore)
      ),
      F.arrayEnumerate(F.groupArray(RowScore))
    )
  ).as(Some("score_datasource"))

  val DSW = F.ifNull(F.any(column("weight")), literal(1.0)).as(Some("datasource_weight"))

  val queryGroupByDS = {
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

    val leftIdsC = F.arrayJoin(F.array((AIDs + AId).map(literal).toSeq)).as(Some("AIDs"))
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
        F.in(B, rightIdsQ.toColumn(None)))
    } else {
      expressionLeft
    }

    val expressionLeftRightWithBFilter = BFilterQ.map(f =>
      F.and(f, expressionLeftRight)
    ).getOrElse(expressionLeftRight)

    val DTAny = F.any(DT).as(Some(DT.rep))

    val withDT = With(DSScore :: DTAny :: DSW :: Nil)
    val selectDSScores = Select(B :: DSW.name :: DTAny.name :: DS :: DSScore.name :: Nil)
    val fromT = From(T, Some("l"))
    val joinWeights = Join(q.toColumn(None), Some("LEFT"), Some("OUTER"), false, Some("r"), DS :: Nil)
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

  def simpleQuery(offset: Int, size: Int) = {
    val leftIdsC = F.arrayJoin(F.array((AIDs + AId).map(literal).toSeq)).as(Some("AIDs"))
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

    val expressionLeftRightWithBFilter = BFilterQ.map(f =>
      F.and(f, expressionLeftRight)
    ).getOrElse(expressionLeftRight)

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

  override val query = {
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
      maxHS.name).as(Some("score"))

    val scoreDSs = F.arrayMap("x -> (x.3, x.1)",collectedDScored.name).as(Some("score_datasources"))
    val scoreDTs = F.arrayMap("x -> (x.4, x.1)",collectedDScored.name).as(Some("score_dt"))
    val uniqDTs = F.groupUniqArray(DT)

    val mappedDTs = F.arrayMap(s"x -> (x, arrayReverseSort(arrayMap(b -> b.2, arrayFilter(a -> a.1 = x,${scoreDTs.name.rep}))))",
      uniqDTs.name).as(Some("mapped_dts"))
    val scoredDTs = F.arrayMap(s"x -> (x.1, arraySum((i, j) -> i / pow(j,2), x.2, arrayEnumerate(x.2)) / ${maxHS.name.rep})",
      mappedDTs.name).as(Some("score_datatypes"))

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
    val fromAgg = From(queryGroupByDS.toColumn(None))
    val groupByB = GroupBy(B :: Nil)
    val orderBySome = orderScoreBy match {
      case Some(p) => OrderBy((
        if (p._2 == "desc") Column(p._1).desc
        else Column(p._1).asc) :: Nil
      )
      case None => OrderBy(scoreOverall.desc :: Nil)
    }

    val limitC = Limit(offset, size)

    val rootQ = Q(withScores, selectScores, fromAgg, groupByB, orderBySome, limitC)
    logger.debug(rootQ.toString)

    rootQ
  }
}
