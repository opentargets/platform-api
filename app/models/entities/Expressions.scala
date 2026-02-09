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
  implicit val getExpressionsResult: GetResult[Expressions] =
    GetResult(r => Json.parse(r.<<[String]).as[Expressions])
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
