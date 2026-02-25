package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import utils.OTLogging
import models.entities.StudyQueryArgs
import models.gql.StudyTypeEnum
import models.entities.CredibleSetQueryArgs

case class CredibleSetByStudyQuery(studyIds: Seq[String],
                                   tableName: String,
                                   studyTableName: String,
                                   offset: Int,
                                   size: Int
) extends Queryable
    with OTLogging {

  def studyLocusIdsByStudyIdSubquery(studyIds: Seq[String]): Query =
    Query(
      Select(
        Functions.arrayJoin(
          column("studyLocusIds")
        ) :: Nil
      ),
      From(column(studyTableName)),
      Where(
        Functions.in(
          column("studyId"),
          Functions.set(studyIds.map(id => literal(id)))
        )
      )
    )

  def subquery(studyId: String): Query = Query(
    Select(
      Column.star :: Functions.countOver("metaTotal") :: Nil
    ),
    From(column(tableName)),
    Where(
      Functions
        .in(
          column("studyLocusId"),
          (
            studyLocusIdsByStudyIdSubquery(Seq(studyId))
          ).toColumn(None)
        )
    ),
    OrderBy(column("studyLocusId").desc :: Nil),
    Limit(offset, size)
  )

  override val query: Query =
    val first = subquery(studyIds.head)
    // add unions for each subsequent studyLocusId
    val querySections: Query = if (studyIds.length == 1) {
      first
    } else {
      studyIds.tail.foldLeft(first) { (acc, id) =>
        Query(acc.sections :+ UnionAll(subquery(id)))
      }
    }
    Query(querySections.sections :+ Format("JSONEachRow"))
}

// Query credible sets by variant IDs, optionally filtered by study types
case class CredibleSetByVariantQuery(variantIds: Seq[String],
                                     studyTypes: Option[Seq[StudyTypeEnum.Value]],
                                     tableName: String,
                                     variantTableName: String,
                                     offset: Int,
                                     size: Int
) extends Queryable
    with OTLogging {

  def whereVariantIds(variantIds: Seq[String]) = Functions.in(
    column("variantId"),
    Functions.set(variantIds.map(id => literal(id)))
  )

  private val whereStudyTypes = studyTypes match {
    case Some(studyTypes) =>
      Functions.in(
        column("studyType"),
        Functions.set(studyTypes.map(st => literal(st.toString)))
      )
    case None =>
      literal(true)
  }

  def studyLocusIdsByVariantIdSubquery(variantIds: Seq[String]): Query =
    Query(
      Select(
        Functions.arrayJoin(
          column("studyLocusIds")
        ) :: Nil
      ),
      From(column(variantTableName)),
      Where(
        whereVariantIds(variantIds)
      )
    )

  def subquery(variantId: String): Query = Query(
    Select(
      Column.star :: Functions
        .countOver("metaTotal") :: literal(variantId).as(Some("metaGroupId")) :: Nil
    ),
    From(column(tableName)),
    Where(
      Functions.and(
        whereStudyTypes,
        Functions.in(
          column("studyLocusId"),
          (
            studyLocusIdsByVariantIdSubquery(Seq(variantId))
          ).toColumn(None)
        )
      )
    ),
    OrderBy(column("studyLocusId").asc :: Nil),
    Limit(offset, size)
  )

  override val query: Query =
    val first = subquery(variantIds.head)
    // add unions for each subsequent studyLocusId
    val querySections: Query = if (variantIds.length == 1) {
      first
    } else {
      variantIds.tail.foldLeft(first) { (acc, id) =>
        Query(acc.sections :+ UnionAll(subquery(id)))
      }
    }
    Query(querySections.sections :+ Format("JSONEachRow"))
}
// Compound credible set query based on studyLocusIds, studyIds, variantIds, studyTypes and regions.
case class CredibleSetQuery(
    queryArgs: CredibleSetQueryArgs,
    tableName: String,
    studyTableName: String,
    variantTableName: String,
    regionTableName: String,
    offset: Int,
    size: Int
) extends Queryable
    with OTLogging {

  def studyLocusIdsByRegionsSubquery(regions: Seq[String]): Query =
    Query(
      Select(
        Functions.arrayJoin(
          column("studyLocusIds")
        ) :: Nil
      ),
      From(column(regionTableName)),
      Where(
        Functions.in(
          column("region"),
          Functions.set(regions.map(region => literal(region)))
        )
      )
    )

  private lazy val studyLocusIdsFromStudyIds: Query =
    CredibleSetByStudyQuery(
      queryArgs.studyIds,
      tableName,
      studyTableName,
      offset,
      size
    ).studyLocusIdsByStudyIdSubquery(queryArgs.studyIds)

  private lazy val studyLocusIdsFromVariantIdsAndStudyTypes: Query =
    CredibleSetByVariantQuery(
      queryArgs.variantIds,
      None,
      tableName,
      variantTableName,
      offset,
      size
    ).studyLocusIdsByVariantIdSubquery(
      queryArgs.variantIds
    )
  private lazy val studyLocusIdsFromRegions: Query =
    studyLocusIdsByRegionsSubquery(queryArgs.regions)

  private lazy val studyLocusIdsQuery: Query = Query(
    Select(
      Functions.arrayJoin(
        Functions.array(
          queryArgs.ids.map(id => literal(id))
        )
      ) :: Nil
    )
  )
  private lazy val studyTypeCondition: Column = queryArgs.studyTypes match {
    case studyTypes if studyTypes.nonEmpty =>
      Functions.in(
        column("studyType"),
        Functions.set(studyTypes.map(st => literal(st.toString)))
      )
    case _ => literal(true)
  }
  private val studyLocusCondition: Column =
    // build the seq of studyLocusId subqueries to then intersect
    val studyLocusIdsQueries = Seq(
      if queryArgs.studyIds.nonEmpty then Some(studyLocusIdsFromStudyIds) else None,
      if queryArgs.variantIds.nonEmpty then Some(studyLocusIdsFromVariantIdsAndStudyTypes)
      else None,
      if queryArgs.regions.nonEmpty then Some(studyLocusIdsFromRegions) else None,
      if queryArgs.ids.nonEmpty then Some(studyLocusIdsQuery) else None
    ).filter(_.isDefined).map(_.get)
    val studyLocusIdsIntersect: Option[Query] =
      if studyLocusIdsQueries.isEmpty then None
      else if studyLocusIdsQueries.length == 1 then Some(studyLocusIdsQueries.head)
      else
        Some(studyLocusIdsQueries.tail.foldLeft(studyLocusIdsQueries.head) { (acc, query) =>
          Query(acc.sections :+ Intersect(query))
        })
    studyLocusIdsIntersect match {
      case Some(q) =>
        Functions.in(
          column("studyLocusId"),
          q.toColumn(None)
        )
      case None => literal(true)
    }

  // we can filter on studyLocusId and then studyType
  private val conditional: Where = Where(
    Functions.and(
      studyLocusCondition,
      studyTypeCondition
    )
  )

  override val query: Query =
    Query(
      Select(
        Column.star ::
          Functions.countOver("metaTotal") :: Nil
      ),
      From(column(tableName)),
      conditional,
      OrderBy(column("studyLocusId").asc :: Nil),
      Limit(offset, size),
      Format("JSONEachRow")
    )

}
