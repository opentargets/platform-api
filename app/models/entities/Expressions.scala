package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import slick.jdbc.GetResult
import utils.db.DbJsonParser.fromPositionedResult

case class Tissue(id: String, label: String, anatomicalSystems: Seq[String], organs: Seq[String])

case class RNAExpression(zscore: Long, value: Double, unit: String, level: Int)

case class CellType(reliability: Boolean, name: String, level: Int)

case class ProteinExpression(reliability: Boolean, level: Int, cellType: Seq[CellType])

case class BaselineExpressionRow(
    targetId: String,
    targetFromSourceId: Option[String],
    tissueBiosampleId: Option[String],
    tissueBiosampleParentId: Option[String],
    tissueBiosampleFromSource: Option[String],
    celltypeBiosampleId: Option[String],
    celltypeBiosampleParentId: Option[String],
    celltypeBiosampleFromSource: Option[String],
    min: Option[Double],
    q1: Option[Double],
    median: Option[Double],
    q3: Option[Double],
    max: Option[Double],
    distribution_score: Double,
    specificity_score: Option[Double],
    datasourceId: String,
    datatypeId: String,
    unit: String,
    qualityControls: Option[String]
)

case class BaselineExpression(
    count: Long,
    rows: Vector[BaselineExpressionRow]
)

object BaselineExpression {
  val empty: BaselineExpression = BaselineExpression(0, Vector.empty)

  implicit val getBaselineExpressionRowFromDB: GetResult[BaselineExpressionRow] =
    GetResult { r =>
      val targetId: String = r.<<
      val targetFromSourceId: Option[String] = r.<<?
      val tissueBiosampleId: Option[String] = r.<<?
      val tissueBiosampleParentId: Option[String] = r.<<?
      val tissueBiosampleFromSource: Option[String] = r.<<?
      val celltypeBiosampleId: Option[String] = r.<<?
      val celltypeBiosampleParentId: Option[String] = r.<<?
      val celltypeBiosampleFromSource: Option[String] = r.<<?
      val min: Option[Double] = r.<<?
      val q1: Option[Double] = r.<<?
      val median: Option[Double] = r.<<?
      val q3: Option[Double] = r.<<?
      val max: Option[Double] = r.<<?
      val distribution_score: Double = r.<<
      val specificity_score: Option[Double] = r.<<?
      val datasourceId: String = r.<<
      val datatypeId: String = r.<<
      val unit: String = r.<<
      val qualityControls: Option[String] = r.<<?

      BaselineExpressionRow(
        targetId,
        targetFromSourceId,
        tissueBiosampleId,
        tissueBiosampleParentId,
        tissueBiosampleFromSource,
        celltypeBiosampleId,
        celltypeBiosampleParentId,
        celltypeBiosampleFromSource,
        min,
        q1,
        median,
        q3,
        max,
        distribution_score,
        specificity_score,
        datasourceId,
        datatypeId,
        unit,
        qualityControls
      )
    }

  implicit val BaselineExpressionRowImp: OFormat[BaselineExpressionRow] =
    Json.format[BaselineExpressionRow]
}
