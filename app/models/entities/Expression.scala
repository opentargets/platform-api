package models.entities

import models.Helpers.logger
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonNaming.SnakeCase

case class Tissue(id: String, label: String, anatomicalSystems: Seq[String], organs: Seq[String])

case class RNAExpression(zscore: Long, value: Double, unit: String, level: Int)

case class CellType(reliability: Boolean, name: String, level: Int)

case class ProteinExpression(reliability: Boolean, level: Int, cellType: Seq[CellType])

case class Expression(tissue: Tissue, rna: RNAExpression, protein: ProteinExpression)

case class Expressions(id: String, rows: Seq[Expression])


object Expression {
  val logger = Logger(this.getClass)

  implicit val tissueW = Json.writes[Tissue]
  implicit val rnaExpressionW = Json.writes[RNAExpression]
  implicit val cellTypeW = Json.writes[CellType]
  implicit val proteinExpressionW = Json.writes[ProteinExpression]
  implicit val expressionW = Json.writes[Expression]
  implicit val expressionsW = Json.writes[Expressions]

  implicit val config = JsonConfiguration(SnakeCase)

  implicit val tissueR: Reads[Tissue] = (
    (__ \ "efo_code").read[String] and
      (__ \ "label").read[String] and
      (__ \ "anatomical_systems").read[Seq[String]] and
      (__ \ "organs").read[Seq[String]]
    ) (Tissue)

  implicit val rnaExpressionR = Json.reads[RNAExpression]
  implicit val cellTypeR = Json.reads[CellType]
  implicit val proteinExpressionR = Json.reads[ProteinExpression]

  implicit val expressionR: Reads[Expression] = (
    __.read[Tissue] and
      (__ \ "rna").read[RNAExpression] and
      (__ \ "protein").read[ProteinExpression]
    ) (Expression.apply _)

  implicit val expressionsR: Reads[Expressions] =
    (
      (__ \ "id").read[String] and
        (__ \ "tissues").readWithDefault[Seq[Expression]](Seq.empty)
      ) (Expressions.apply _)
}


