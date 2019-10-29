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

  def mkHSColumn(col: Column, maxHS: Column): IndexedSeq[Column] = {
    val colName = Some(col.name.toString + "_v")

    val dsV = arraySlice(arrayReverseSort(flatten(groupArray(col.name))),1, maxVectorElementsDefault)
      .as(colName)

    val dsVHS = div(arraySum("(a, b) -> a / pow((b + 1),2)", dsV.name, range(length(dsV.name))), maxHS)
        .as(colName.map(_ + "_hs"))

    Vector(dsV, dsVHS)
  }
}