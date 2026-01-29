package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import play.api.Logging
import play.libs.F
import models.gql.InteractionSourceEnum

case class InteractionSourcesQuery(
    tableName: String
) extends Queryable
    with Logging {

  override val query: Query =
    Query(
      Select(
        Seq(
          column("resource.databaseVersion").as(Some("databaseVersion")),
          column("resource.sourceDatabase").as(Some("sourceDatabase"))
        ),
        distinct = true
      ),
      From(column(tableName)),
      ArrayJoin(
        column("evidences.interactionResources").as(Some("resource"))
      ),
      Where(
        Column.inSet("resource.sourceDatabase", InteractionSourceEnum.values.map(_.toString).toSeq)
      ),
      OrderBy(column("resource.sourceDatabase").asc :: Nil),
      Limit(0, InteractionSourceEnum.values.size),
      Format("JSONEachRow")
    )
}
