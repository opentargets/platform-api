package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import utils.OTLogging

case class EvidenceQuery(evidenceTable: String, subQuery: Query, offset: Int, limit: Int)
    extends Queryable {
  override val query: Query = Query(
    Select(Column.star :: Functions.countOver("metaTotal") :: Nil),
    From(column(evidenceTable)),
    Where(
      Functions.in(
        column("id"),
        subQuery.toColumn(None)
      )
    ),
    OrderBy(column("score").desc :: column("id").asc :: Nil),
    Limit(offset, limit),
    Format("JSONEachRow"),
    Settings(Map("output_format_json_escape_forward_slashes" -> "0"))
  )
}

object EvidenceQuery extends OTLogging {
  private def datasourceFilter(datasourceIds: Option[Seq[String]]) = datasourceIds match {
    case Some(dsIds) =>
      Column.inSet(
        "datasourceId",
        dsIds
      )
    case None =>
      literal(true)
  }
  def byVariant(
      variantId: String,
      datasourceIds: Option[Seq[String]],
      tableName: String,
      variantJoinTableName: String,
      offset: Int,
      limit: Int
  ): EvidenceQuery = {
    val subquery = Query(
      Select(column("id") :: Nil),
      From(column(variantJoinTableName)),
      Where(
        Functions.and(
          Functions.equals(column("variantId"), literal(variantId)),
          datasourceFilter(datasourceIds)
        )
      )
    )
    EvidenceQuery(tableName, subquery, offset, limit)
  }

  def byDiseaseTarget(
      targetIds: Seq[String],
      diseaseIds: Seq[String],
      datasourceIds: Option[Seq[String]],
      tableName: String,
      diseaseTargetJoinTableName: String,
      offset: Int,
      limit: Int
  ): EvidenceQuery = {
    val subquery = Query(
      Select(column("id") :: Nil),
      From(column(diseaseTargetJoinTableName)),
      Where(
        Functions.and(
          Functions.in(
            column("targetId"),
            Functions.set(targetIds.map(literal).toSeq)
          ),
          Functions.in(
            column("diseaseId"),
            Functions.set(diseaseIds.map(literal).toSeq)
          ),
          datasourceFilter(datasourceIds)
        )
      )
    )
    EvidenceQuery(tableName, subquery, offset, limit)
  }
}
