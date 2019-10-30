package models.entities

import java.util.UUID

import scala.math.pow
import elesecu._
import elesecu.Column._
import elesecu.Functions._
import elesecu.{Functions => F}
import models.entities.Configuration.DatasourceSettings

object Harmonic {
  val maxVectorElementsDefault: Int = 100
  val pExponentDefault: Int = 2

  def maxValue(vSize: Int, pExponent: Int, maxScore: Double): Double =
    (0 until vSize).foldLeft(0D)((acc: Double, n: Int) => acc + (maxScore / pow(1D + n,pExponent)))

  lazy val maxHSColumn: Column =
    literal(Harmonic.maxValue(maxVectorElementsDefault, pExponentDefault, 1.0)).as(Some("max_hs"))

  def mkHSColumn(col: Column, maxHS: Column): Seq[Column] = {
    val colName = Some(col.name.toString + "_v")

    val dsV = arraySlice(arrayReverseSort(flatten(groupArray(col.name))),1, maxVectorElementsDefault)
      .as(colName)

    val dsVHS = divide(arraySum("(a, b) -> a / pow((b + 1),2)", dsV.name, range(length(dsV.name))), maxHS)
        .as(colName.map(_ + "_hs"))

    Vector(dsV, dsVHS)
  }

  def mkQuery(fixedCol: String,
              queryCol: String,
              queryColValue: String,
              table: String,
              datasources: Seq[DatasourceSettings]): Query = {
    //with
    //    1.6349839001848923 as max_hs,
    //    arrayReverseSort(flatten(groupArray(datasource_europepmc.scores))) as europepmc,
    //    arrayReverseSort(flatten(groupArrayIf(datasource_expression_atlas.scores, disease_id = 'EFO_0000616'))) as expression_atlas,
    //
    //    arraySlice(europepmc, 1, 100) as ds_epmc_v,
    //        arraySum((a, b) -> a / pow((b + 1),2), ds_epmc_v, range(length(ds_epmc_v))) / max_hs as ds_europepmc_hs,
    //    arraySlice(expression_atlas, 1, 100) as ds_expression_atlas_v,
    //        arraySum((a, b) -> a / pow((b + 1),2), ds_expression_atlas_v, range(length(ds_expression_atlas_v))) / max_hs as ds_expression_atlas_hs,
    //
    //    array(ds_europepmc_hs, ds_expression_atlas_hs) as DS_V,
    //    array(0.2, 0.2) as hs_v_scores,
    //    arrayReverseSort(arrayMap((a, b) -> a * b, DS_V, hs_v_scores)) as HSV,
    //    arraySum((a, b) -> a / pow((b + 1),2),arrayReverseSort(HSV),range(length(HSV))) / max_hs as HS
    //select
    //    target_id,
    //    HS,
    //    DS_V,
    //    groupArray(disease_id) as disease_ids
    //from ot.aotf_d
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
    val qCol = Column(queryCol)

    val hsMaxValueCol = Harmonic.maxHSColumn
    val dsHSCols = datasources.flatMap(c => Harmonic.mkHSColumn(Column(c.id), hsMaxValueCol))
    val dsWeightV = array(datasources.map(c => literal(c.weight))).as(Some("ds_scores"))

    val dsV = array(dsHSCols.withFilter(_.name.rep.endsWith("_v_hs")).map(_.name))
      .as(Some("dsV"))

    val overallHS = arrayReverseSort(dsV)

    val w = With(hsMaxValueCol +: dsWeightV +: dsHSCols)
    val s = Select(idCol +: dsV +: dsWeightV.name +: Nil)
    val pw = PreWhere(F.equals(qCol, literal(queryColValue)))
    val f = From(Column(table))
    Query(w, s, f, pw)
  }
}