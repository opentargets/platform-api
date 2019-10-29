package models.entities

import java.util.UUID

import scala.math.pow
import elesecu._
import elesecu.Column._

object Harmonic {
  val maxVectorElementsDefault: Int = 100
  val pExponentDefault: Int = 2

  def maxValue(vSize: Int, pExponent: Int, maxScore: Double): Double =
    (0 until vSize).foldLeft(0D)((acc: Double, n: Int) => acc + (maxScore / pow(1D + n,pExponent)))

  def maxHSColumn: Column =
    lit(Harmonic.maxValue(maxVectorElementsDefault, pExponentDefault, 1.0)).as(Some("max_hs"))

  def mkHSColumn(col: Column, maxHS: Column): Seq[Column] = {
    //    1.6349839001848923 as max_hs,
    //    2.6731723538638033 as max_hs_total,
    //    arrayReverseSort(flatten(groupArray(datasource_europepmc.scores))) as europepmc,
    //    arrayReverseSort(flatten(groupArray(datasource_cancer_gene_census.scores))) as cancer_gene_census,
    //    arrayReverseSort(flatten(groupArrayIf(datasource_chembl.scores, target_id = 'ENSG00000091831'))) as chembl,
    //    arrayReverseSort(flatten(groupArray(datasource_crispr.scores))) as crispr,
    //    arrayReverseSort(flatten(groupArray(datasource_eva.scores))) as eva,
    //    arrayReverseSort(flatten(groupArray(datasource_eva_somatic.scores))) as eva_somatic,
    //    arrayReverseSort(flatten(groupArray(datasource_expression_atlas.scores))) as expression_atlas,
    //
    //     arraySlice(europepmc, 1, 100) as ds_epmc_v,
    //     arraySum((a, b) -> a / pow((b + 1),2), ds_epmc_v, range(length(ds_epmc_v))) / max_hs as ds_europepmc_hs,

    val colName = col.alias match {
      case Some(n) => Some(n + "_v")
      case None => Some(UUID.randomUUID().toString)
    }
    val dsV = arraySlice(arrayReverseSort(flatten(groupArray(col))),1, maxVectorElementsDefault)
      .as(colName)

    val dsVHS = div(arraySum("(a, b) -> a / pow((b + 1),2)", dsV, range(length(dsV))), maxHS)
        .as(colName.map(_ + "_hs"))

    List(dsV, dsVHS)

  }
}