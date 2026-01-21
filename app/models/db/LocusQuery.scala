package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import play.api.Logging

case class LocusQuery(studyLocusIds: Seq[String],
                      variantIds: Option[Seq[String]],
                      tableName: String,
                      offset: Int,
                      size: Int
) extends Queryable
    with Logging {

  private val variantCondition: Column = variantIds match {
    case Some(vids) =>
      Functions.in(
        column("variantId"),
        Functions.set(vids.map(literal).toSeq)
      )
    case None => literal(true)
  }
  def locusQuery(studyLocusId: String, variantIds: Option[Seq[String]]): Query = Query(
    Select(
      Column.star :: Functions.countOver("metaTotal") :: Nil
    ),
    From(column(tableName)),
    Where(
      Functions.and(
        Functions.equals(column("studyLocusId"), literal(studyLocusId)),
        variantCondition
      )
    ),
    OrderBy(column("studyLocusId").desc :: Nil),
    Limit(offset, size)
  )

  override val query: Query =
    val first = locusQuery(studyLocusIds.head, variantIds)
    // add unions for each subsequent studyLocusId
    val querySections: Query = if (studyLocusIds.length == 1) {
      first
    } else {
      studyLocusIds.tail.foldLeft(first) { (acc, id) =>
        Query(acc.sections :+ UnionAll(locusQuery(id, variantIds)))
      }
    }
    Query(querySections.sections :+ Format("JSONEachRow"))

}
