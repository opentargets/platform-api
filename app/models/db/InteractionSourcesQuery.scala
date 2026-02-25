package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import utils.OTLogging
import play.libs.F
import models.gql.InteractionSourceEnum

case class InteractionSourcesQuery(
    tableName: String
) extends Queryable
    with OTLogging {

  override val query: Query =
    Query(
      Select(Column.star :: Nil),
      From(
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
            Column.inSet("resource.sourceDatabase",
                         InteractionSourceEnum.values.map(_.toString).toSeq
            )
          ),
          Limit(0, InteractionSourceEnum.values.size)
        ).toColumn(None)
      ),
      OrderBy(column("sourceDatabase").asc :: Nil),
      Format("JSONEachRow")
    )
}
