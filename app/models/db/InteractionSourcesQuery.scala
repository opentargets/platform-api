package models.db

import esecuele.Column.column
import esecuele._
import utils.OTLogging
import models.gql.InteractionSourceEnum

case class InteractionSourcesQuery(
    tableName: String
) extends Queryable
    with OTLogging {

  val interaction_subset: Column = Query(
    Select(Column.star :: Nil),
    From(column(tableName)),
    Limit(0, 1000)
  ).toColumn(Some("t"))

  override val query: Query =
    Query(
      Select(Column.star :: Nil),
      From(
        Query(
          Select(
            Seq(
              column("evidence.interactionResources.databaseVersion").as(Some("databaseVersion")),
              column("evidence.interactionResources.sourceDatabase").as(Some("sourceDatabase"))
            ),
            distinct = true
          ),
          From(interaction_subset),
          ArrayJoin(
            column("t.interactions").as(Some("interaction"))
          ),
          ArrayJoin(
            column("interaction.evidences").as(Some("evidence"))
          ),
          Limit(0, InteractionSourceEnum.values.size)
        ).toColumn(None)
      ),
      OrderBy(column("sourceDatabase").asc :: Nil),
      Format("JSONEachRow")
    )
}
