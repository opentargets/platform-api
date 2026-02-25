package models.db
import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import utils.OTLogging
import models.gql.{StudyTypeEnum, InteractionSourceEnum}

enum sortDirection:
  case ASC, DESC

case class OrderBy(field: String, direction: sortDirection)

case class OneToMany(ids: Seq[String],
                     idField: String,
                     arrayField: String,
                     tableName: String,
                     offset: Int,
                     size: Int,
                     filter: Option[Column] = None,
                     sortBy: Option[OrderBy] = None,
                     selectAlso: Seq[Column] = Nil
) extends Queryable
    with OTLogging {

  private val filteredArray: Column = filter match {
    case Some(f) => f
    case None    => column(arrayField)
  }

  private val filteredAndSortedArray: Column = sortBy match {
    case Some(order) =>
      order.direction match {
        case sortDirection.ASC  => Functions.arraySort(filteredArray)
        case sortDirection.DESC => Functions.reverse(Functions.arraySort(filteredArray))
      }
    case None =>
      filteredArray
  }

  override val query: Query =
    Query(
      With(
        filteredAndSortedArray ::
          Functions.cast(Functions.length(filteredAndSortedArray), "UInt32").as(Some("count")) ::
          Functions
            .arraySlice(filteredAndSortedArray, offset + 1, size)
            .as(Some("rows")) ::
          Nil
      ),
      Select(
        (column("count") :: column("rows") ::
          column(idField).as(Some("id")) :: Nil) ++ selectAlso
      ),
      From(column(tableName)),
      Where(
        Functions.in(
          column(idField),
          Functions.set(ids.map(id => literal(id)))
        )
      ),
      Format("JSONEachRow"),
      Settings(Map("output_format_json_escape_forward_slashes" -> "0"))
    )
}

object OneToMany {
  def colocQuery(studyLocusIds: Seq[String],
                 studyType: Seq[StudyTypeEnum.Value],
                 tableName: String,
                 offset: Int,
                 size: Int
  ): OneToMany =

    val filter: Option[Column] =
      Some(
        Functions.arrayFilter(
          s"c -> (${Functions
              .in(
                column("c.rightStudyType"),
                Functions.set(studyType.map(st => literal(st.toString)))
              )})",
          column("colocalisation")
        )
      )
    OneToMany(
      studyLocusIds,
      "studyLocusId",
      "colocalisation",
      tableName,
      offset,
      size,
      filter
    )
  def interactionQuery(targetIds: Seq[String],
                       tableName: String,
                       scoreThreshold: Option[Double],
                       sourceDatabase: Option[InteractionSourceEnum.Value],
                       offset: Int,
                       size: Int
  ): OneToMany =
    val filter: Option[Column] =
      Some(
        Functions.arrayFilter(
          s"i -> (${Functions.and(
              scoreThreshold match {
                case Some(threshold) =>
                  Functions.or(
                    Functions.greaterOrEquals(column("i.scoring"), literal(threshold)),
                    Functions.isNull(column("i.scoring"))
                  )
                case None => literal(true)
              },
              sourceDatabase match {
                case Some(dbName) =>
                  Functions.equals(column("i.sourceDatabase"), literal(dbName.toString()))
                case None => literal(true)
              }
            )})",
          column("interactions")
        )
      )
    OneToMany(
      targetIds,
      "targetA",
      "interactions",
      tableName,
      offset,
      size,
      filter,
      sortBy = Some(OrderBy("i.scoring", sortDirection.DESC))
    )

  def l2gQuery(studyLocusIds: Seq[String], tableName: String, offset: Int, size: Int): OneToMany =
    OneToMany(
      studyLocusIds,
      "studyLocusId",
      "l2g_predictions",
      tableName,
      offset,
      size
    )

  def locusQuery(studyLocusIds: Seq[String],
                 tableName: String,
                 variantIds: Option[Seq[String]],
                 offset: Int,
                 size: Int
  ): OneToMany =
    val filter: Option[Column] = variantIds match {
      case Some(vids) =>
        Some(
          Functions.arrayFilter(
            s"l -> (${Functions
                .in(
                  column("l.variantId"),
                  Functions.set(vids.map(id => literal(id)))
                )
                .toString})",
            column("locus")
          )
        )
      case None => None
    }
    OneToMany(studyLocusIds, "studyLocusId", "locus", tableName, offset, size, filter)
}
