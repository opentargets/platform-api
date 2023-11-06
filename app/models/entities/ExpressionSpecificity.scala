package models.entities

import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class ExpressionListItem(bodyPartId: String,
                              bodyPartLevel: String,
                              bodyPartName: String,
                              tpm: Double
)
case class AdatissScoreListItem(bodyPartId: String,
                                bodyPartLevel: String,
                                bodyPartName: String,
                                adatissScore: Double
)
case class ExpressionSpecificity(adatissScores: Seq[AdatissScoreListItem],
                                 gini: Double,
                                 hpaDistribution: String,
                                 hpaSpecificity: String
)
case class BaselineExpression(ensemblGeneId: String,
                              expression: Seq[ExpressionListItem],
                              expressionSpecificity: ExpressionSpecificity
)

object BaselineExpression {
  implicit val expressionListItemFormat: OFormat[ExpressionListItem] =
    Json.format[ExpressionListItem]
  implicit val adatissScoreFormat: OFormat[AdatissScoreListItem] = Json.format[AdatissScoreListItem]
  implicit val expressionSpecificityListItemFormat: OFormat[ExpressionSpecificity] =
    Json.format[ExpressionSpecificity]
  implicit val baselineExpressionFormat: OFormat[BaselineExpression] =
    Json.format[BaselineExpression]
}
