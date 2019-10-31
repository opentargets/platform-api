package models.entities

import java.util.UUID

import scala.math.pow
import elesecu._
import elesecu.Column._
import elesecu.Functions._
import elesecu.{Functions => F}
import models.entities.Configuration.{DatasourceSettings, LUTableSettings}

object Harmonic {
  private val maxVectorElementsDefault: Int = 100
  private val pExponentDefault: Int = 2

  private def maxValue(vSize: Int, pExponent: Int, maxScore: Double): Double =
    (0 until vSize).foldLeft(0D)((acc: Double, n: Int) => acc + (maxScore / pow(1D + n,pExponent)))

  private def mkHSColumn(col: Column,
                         maxHS: Column,
                         propagateCondition: Option[Column]): Seq[Column] = {
    val colName = Some(col.name.toString.replaceAll("\\.", "__") + "_v")

    /*
      WARN! this is a hack that needs to be removed as we dont want
      to keep applying it. This is basically done for expression atlas
      but it is a temporal action although still coming from years ago.
      There is a will to remove it let's see if that happens
     */
    val ga = if (propagateCondition.isDefined) {
      groupArrayIf(col.name, propagateCondition.get)
    } else {
      groupArray(col.name)
    }

    val dsV = arraySlice(arrayReverseSort(flatten(ga)),1, maxVectorElementsDefault)
      .as(colName)

    val dsVHS = divide(arraySum("(a, b) -> a / pow((b + 1),2)", dsV.name, range(length(dsV.name))), maxHS)
        .as(colName.map(_ + "_hs"))

    Vector(dsV, dsVHS)
  }

  // TODO network expansion
  def apply(fixedCol: String,
            queryColName: String,
            queryColValue: String,
            table: String,
            datasources: Seq[DatasourceSettings],
            expansionTable: Option[LUTableSettings]): Query = {
    //prewhere
    //    (disease_id = 'EFO_0000616' or
    //     disease_id in
    //     ( select neighbour
    //       from (select joinGet('ot.disease_network_t', 'neighbours', 'EFO_0000616') as neighbours)
    //                array join neighbours as neighbour))
    //group by target_id
    //having HS > 0
    //order by HS desc;

    val idCol = Column(fixedCol)
    val qColValueCol = literal(queryColValue)
    val qCol = Column(queryColName)

    // WARN! the propagation hack strikes again about expression atlas
    val booleanCondition = Some(F.equals(qCol, qColValueCol))
    val hsMaxValueCol = literal(Harmonic.maxValue(maxVectorElementsDefault, pExponentDefault, 1.0))
      .as(Some("max_hs_score"))

    // WARN! the propagation hack strikes again about expression atlas
    val dsHSCols = datasources.flatMap(c => {
      if (c.id == "expression_atlas")
        Harmonic.mkHSColumn(Column(c.id), hsMaxValueCol, booleanCondition)
      else
        Harmonic.mkHSColumn(Column(c.id), hsMaxValueCol, None)
    })

    val dsWeightV = array(datasources.map(c => literal(c.weight))).as(Some("ds_scores_v"))
    val dsV = array(dsHSCols.withFilter(_.name.rep.endsWith("_v_hs")).map(_.name))
      .as(Some("ds_v"))

    val dsVWeighted = arrayReverseSort(arrayMap("(a, b) -> a * b", dsV, dsWeightV)).as(Some("wds_v"))
    val overallHS = F.divide(arraySum("(a, b) -> a / pow((b + 1), 2)",
        dsVWeighted.name,
        range(length(dsVWeighted.name))
      ),
      hsMaxValueCol.name
    ).as(Some("overall_hs"))

    val idsV = groupArray(qCol).as(Some("ids_v"))

    val w = With(hsMaxValueCol +: dsWeightV +: dsHSCols :+ dsV :+ dsVWeighted :+ overallHS :+ idsV)
    val s = Select(idCol.name +: overallHS.name +: dsV.name +: idsV.name +: Nil)
    val f = From(Column(table))

    val expansionQuery = expansionTable.map(lut => {
      val expCol = F.joinGet(lut.name, lut.field.get, qColValueCol).as(lut.field)
      val neighbourCol = expCol.name.as(Some("neighbour"))
      val innerSel = Query(Select(expCol +: Nil), ArrayJoin(neighbourCol))
      val sel = Query(Select(neighbourCol.name +: Nil), From(innerSel.toColumn)).toColumn
      val inn = F.in(qCol, sel)
      F.or(F.equals(qCol, qColValueCol), inn)
    })

    val pw = PreWhere(expansionQuery.getOrElse(F.equals(qCol, qColValueCol)))
    val g = GroupBy(idCol +: Nil)
    val h = Having(F.greater(overallHS.name, literal(0.0)))
    val o = OrderBy(overallHS.name.desc +: Nil)
    Query(w, s, f, pw, g, h, o)
  }
}