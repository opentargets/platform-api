package models.db
import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import play.api.Logging
import models.gql.{StudyTypeEnum, InteractionSourceEnum}

enum sortDirection:
  case ASC, DESC

case class orderBy(field: String, direction: sortDirection)

/** Query to get rows where index is not unique (one to many mapping)
  *
  * @param values
  *   list of values to query
  * @param index
  *   name of the index field in the table
  * @param tableName
  *   name of the table to query
  * @param offset
  *   pagination offset
  * @param size
  *   pagination size
  * @param additionalFilter
  *   optional additional filter to apply to each query
  * @param orderBy
  *   optional sorting parameter to sort results by a specific field and direction
  */

case class OneToManyQuery(values: Seq[String],
                          index: String,
                          tableName: String,
                          offset: Int,
                          size: Int,
                          additionalFilter: Option[Column] = None,
                          orderBy: Option[orderBy] = None,
                          countField: Option[String] = None
) extends Queryable
    with Logging {

  private val selectSection: Select = countField match {
    case Some(cf) =>
      Select(
        Column.star :: Functions.countOver(cf) :: Nil
      )
    case None =>
      Select(
        Column.star :: Nil
      )
  }
  private val orderBySection: OrderBy = orderBy match {
    case None => OrderBy(column(index).asc :: Nil)
    case Some(s) =>
      val col = s.direction match {
        case sortDirection.ASC  => column(s.field).asc
        case sortDirection.DESC => column(s.field).desc
      }
      OrderBy(col :: Nil)
  }
  def singleQuery(value: String, index: String): Query =
    Query(
      selectSection,
      From(column(tableName)),
      Where(
        additionalFilter match {
          case Some(filter) =>
            Functions.and(
              Functions.equals(column(index), literal(value)),
              filter
            )
          case None =>
            Functions.equals(column(index), literal(value))
        }
      ),
      orderBySection,
      Limit(offset, size)
    )

  override val query: Query =
    val first = singleQuery(values.head, index)
    // add unions for each subsequent query
    val querySections: Query = if (values.length == 1) {
      first
    } else {
      values.tail.foldLeft(first) { (acc, id) =>
        Query(acc.sections :+ UnionAll(singleQuery(id, index)))
      }
    }
    Query(querySections.sections :+ Format("JSONEachRow"))
}

object OneToManyQuery {
  def colocQuery(studyLocusIds: Seq[String],
                 studyType: Seq[StudyTypeEnum.Value],
                 tableName: String,
                 offset: Int,
                 size: Int
  ): OneToManyQuery =
    OneToManyQuery(
      studyLocusIds,
      "studyLocusId",
      tableName,
      offset,
      size,
      Some(Column.inSet("rightStudyType", studyType.map(st => st.toString))),
      countField = Some("metaTotal")
    )

  def locusQuery(studyLocusIds: Seq[String],
                 tableName: String,
                 variantIds: Option[Seq[String]],
                 offset: Int,
                 size: Int
  ): OneToManyQuery =
    OneToManyQuery(
      studyLocusIds,
      "studyLocusId",
      tableName,
      offset,
      size,
      variantIds match {
        case Some(vids) => Some(Column.inSet("variantId", vids))
        case None       => None
      },
      countField = Some("metaTotal")
    )

  def interactionQuery(targetIds: Seq[String],
                       tableName: String,
                       scoreThreshold: Option[Double],
                       sourceDatabase: Option[InteractionSourceEnum.Value],
                       offset: Int,
                       size: Int
  ): OneToManyQuery =
    OneToManyQuery(
      targetIds,
      "targetA",
      tableName,
      offset,
      size,
      Some(
        Functions.and(
          scoreThreshold match {
            case Some(threshold) =>
              Functions.or(
                Functions.greaterOrEquals(column("scoring"), literal(threshold)),
                Functions.isNull(column("scoring"))
              )
            case None => literal(true)
          },
          sourceDatabase match {
            case Some(dbName) =>
              Functions.equals(column("sourceDatabase"), literal(dbName.toString()))
            case None => literal(true)
          }
        )
      ),
      orderBy = Some(orderBy("scoring", sortDirection.DESC)),
      countField = Some("metaTotal")
    )
}
