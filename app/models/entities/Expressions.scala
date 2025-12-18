package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import slick.jdbc.GetResult

case class Tissue(id: String, label: String, anatomicalSystems: Seq[String], organs: Seq[String])

case class RNAExpression(zscore: Long, value: Double, unit: String, level: Int)

case class CellType(reliability: Boolean, name: String, level: Int)

case class ProteinExpression(reliability: Boolean, level: Int, cellType: Seq[CellType])

case class Expression(tissue: Tissue, rna: RNAExpression, protein: ProteinExpression)

case class Expressions(id: String, rows: Seq[Expression])

object Expressions {
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

  implicit val tissueW: OWrites[Tissue] = Json.writes[Tissue]
  implicit val rnaExpressionW: OWrites[RNAExpression] = Json.writes[RNAExpression]
  implicit val cellTypeW: OWrites[CellType] = Json.writes[CellType]
  implicit val proteinExpressionW: OWrites[ProteinExpression] = Json.writes[ProteinExpression]
  implicit val expressionW: OWrites[Expression] = Json.writes[Expression]
  implicit val expressionsW: OWrites[Expressions] = Json.writes[Expressions]

  implicit val tissueR: Reads[Tissue] = (
    (__ \ "efo_code").read[String] and
      (__ \ "label").read[String] and
      (__ \ "anatomical_systems").read[Seq[String]] and
      (__ \ "organs").read[Seq[String]]
  )(Tissue.apply)

  implicit val rnaExpressionR: Reads[RNAExpression] = Json.reads[RNAExpression]
  implicit val cellTypeR: Reads[CellType] = Json.reads[CellType]
  implicit val proteinExpressionR: Reads[ProteinExpression] = Json.reads[ProteinExpression]

  implicit val expressionR: Reads[Expression] = (
    __.read[Tissue] and
      (__ \ "rna").read[RNAExpression] and
      (__ \ "protein").read[ProteinExpression]
  )(Expression.apply)

  implicit val expressionsR: Reads[Expressions] =
    (
      (__ \ "id").read[String] and
        (__ \ "tissues").readWithDefault[Seq[Expression]](Seq.empty)
    )(Expressions.apply)
}

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
    unit: String
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
        unit
      )
    }

  implicit val BaselineExpressionRowImp: OFormat[BaselineExpressionRow] =
    Json.format[BaselineExpressionRow]
}
