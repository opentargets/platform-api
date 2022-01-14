package models.entities

import scala.math.pow
import esecuele._
import esecuele.Column._
import esecuele.Functions._
import esecuele.{Functions => F}
import models.entities.Configuration.{DatasourceSettings, LUTableSettings}

object Harmonic {

  val maxVectorElementsDefault: Int = 100
  val pExponentDefault: Int = 2

  def maxValue(vSize: Int, pExponent: Int, maxScore: Double): Double =
    (0 until vSize).foldLeft(0d)((acc: Double, n: Int) => acc + (maxScore / pow(1d + n, pExponent)))

  private def mkHSColumn(
      col: Column,
      maxHS: Column,
      propagateCondition: Option[Column]
  ): Seq[Column] = {
    val colName = Some(col.name.toString.replaceAll("\\.", "__") + "_v")

    /*
      WARN! this is a hack that needs to be removed as we dont want
      to keep applying it. This is basically done for expression atlas
      but it is a temporal action although still coming from years ago.
      There is a will to remove it let's see if that happens
     */
    val ga = propagateCondition
      .flatMap(x => Some(groupArrayIf(col.name, x)))
      .getOrElse(groupArray(col.name))

    val dsV = arraySlice(arrayReverseSort(None, flatten(ga)), 1, maxVectorElementsDefault)
      .as(colName)

    val dsVHS =
      divide(
        arraySum(Some("(a, b) -> a / pow((b + 1),2)"), dsV.name, range(length(dsV.name))),
        maxHS
      )
        .as(colName.map(_ + "_hs"))

    Vector(dsV, dsVHS)
  }

  /** Harmonic CH SQL dsl to compute associations on the fly
    *
    * @param fixedCol       if your `queryColname` is target_id it needs to be disease_id
    * @param queryColName   the other entity you are returning from `fixedCol` so target_id
    * @param table          the table name used for the computation
    * @param datasources    the configuration of datasources that are used to compute all harmonics
    * @param expansionTable it is used if Some
    * @param pagination     page and size to return
    */
  def apply(
      fixedCol: String,
      queryColName: String,
      queryColValue: String,
      table: String,
      datasources: Seq[DatasourceSettings],
      expansionTable: Option[LUTableSettings],
      pagination: Pagination
  ): Query = {
    val idCol = Column(fixedCol)
    val qColValueCol = literal(queryColValue)
    val qCol = Column(queryColName)

    // WARN! the propagation hack strikes again about expression atlas
    val booleanCondition = Some(F.equals(qCol, qColValueCol))
    val hsMaxValueCol = literal(Harmonic.maxValue(maxVectorElementsDefault, pExponentDefault, 1.0))
      .as(Some("max_hs_score"))

    // WARN! the propagation hack strikes again about expression atlas
    val dsHSCols = datasources.flatMap { c =>
      if (c.id == "expression_atlas")
        Harmonic.mkHSColumn(Column(c.id), hsMaxValueCol, booleanCondition)
      else
        Harmonic.mkHSColumn(Column(c.id), hsMaxValueCol, None)
    }

    val dsWeightV = array(datasources.map(c => literal(c.weight))).as(Some("ds_scores_v"))
    val dsV = array(dsHSCols.withFilter(_.name.rep.endsWith("_v_hs")).map(_.name))
      .as(Some("ds_v"))

    val dsVWeighted =
      arrayReverseSort(None, arrayMap("(a, b) -> a * b", dsV, dsWeightV)).as(Some("wds_v"))
    val overallHS = F
      .divide(
        arraySum(
          Some("(a, b) -> a / pow((b + 1), 2)"),
          dsVWeighted.name,
          range(length(dsVWeighted.name))
        ),
        hsMaxValueCol.name
      )
      .as(Some("overall_hs"))

    val idsV = groupArray(qCol).as(Some("ids_v"))

    val w = With(hsMaxValueCol +: dsWeightV +: dsHSCols :+ dsV :+ dsVWeighted :+ overallHS :+ idsV)
//    val s = Select(idCol.name +: overallHS.name +: dsV.name +: idsV.name +: Nil)
    val s = Select(idCol.name +: overallHS.name +: dsV.name +: Nil)
    val f = From(Column(table))

    val expansionQuery = expansionTable.map { lut =>
      val expCol = F.joinGet(lut.name, lut.field.get, qColValueCol).as(lut.field)
      val neighbourCol = expCol.name.as(Some("neighbour"))
      val innerSel = Query(Select(expCol +: Nil))
      val sel = Query(
        Select(neighbourCol.name +: Nil),
        From(innerSel.toColumn(None)),
        ArrayJoin(neighbourCol)
      ).toColumn(None)
      val inn = F.in(qCol, sel)
      F.or(F.equals(qCol, qColValueCol), inn)
    }

    val pw = PreWhere(expansionQuery.getOrElse(F.equals(qCol, qColValueCol)))
    val g = GroupBy(idCol +: Nil)
    val h = Having(F.greater(overallHS.name, literal(0.0)))
    val o = OrderBy(overallHS.name.desc +: Nil)
    val l = Limit(pagination.offset, pagination.size)
    Query(w, s, f, pw, g, h, o, l)
  }
}
