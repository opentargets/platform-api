package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import play.api.Logging
import play.libs.F
import models.gql.StudyTypeEnum

case class ColocalisationQuery(studyLocusIds: Seq[String],
                               studyType: Seq[StudyTypeEnum.Value],
                               tableName: String,
                               offset: Int,
                               size: Int
) extends Queryable
    with Logging {

  def colocQuery(studyLocusId: String): Query = Query(
    Select(
      Column.star :: Functions.countOver("metaTotal") :: Nil
    ),
    From(column(tableName)),
    Where(
      Functions.and(
        Functions.equals(column("studyLocusId"), literal(studyLocusId)),
        Functions.in(column("rightStudyType"),
                     Functions.set(studyType.map(st => literal(st.toString)))
        )
      )
    ),
    Limit(offset, size)
  )

  override val query: Query =
    val first = colocQuery(studyLocusIds.head)
    // add unions for each subsequent studyLocusId
    val querySections: Query = if (studyLocusIds.length == 1) {
      first
    } else {
      studyLocusIds.tail.foldLeft(first) { (acc, id) =>
        Query(acc.sections :+ UnionAll(colocQuery(id)))
      }
    }
    Query(querySections.sections :+ Format("JSONEachRow"))

}
