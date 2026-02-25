package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import utils.OTLogging
import models.entities.StudyQueryArgs

case class StudiesQuery(queryArgs: StudyQueryArgs,
                        tableName: String,
                        diseaseTableName: String,
                        offset: Int,
                        size: Int
) extends Queryable
    with OTLogging {

  private val studyIdColumns: Column = if (queryArgs.enableIndirect) {
    Functions.arrayUnion(Seq(column("studyIds"), column("indirectStudyIds")))
  } else {
    column("studyIds")
  }

  private val studyIdsFromDiseasesSubquery: Query =
    Query(
      Select(
        Functions.arrayJoin(
          studyIdColumns
        ) :: Nil
      ),
      From(column(diseaseTableName)),
      Where(
        Functions.in(
          column("id"),
          Functions.set(queryArgs.diseaseIds.map(literal).toSeq)
        )
      )
    )

  private val studyIdsFromDiseaseIds =
    Functions.in(
      column("studyId"),
      studyIdsFromDiseasesSubquery.toColumn(None)
    )

  private val studyIdsOnly =
    Functions.in(
      column("studyId"),
      Functions.set(queryArgs.id.map(literal).toSeq)
    )

  private val conditional =
    queryArgs match {
      case justDiseaseIds if queryArgs.id.isEmpty =>
        Where(studyIdsFromDiseaseIds)
      case justStudyIds if queryArgs.diseaseIds.isEmpty =>
        Where(studyIdsOnly)
      case bothDiseaseAndStudyIds =>
        Where(
          Functions.and(
            studyIdsFromDiseaseIds,
            studyIdsOnly
          )
        )
    }

  override val query: Query =
    Query(
      Select(
        Column.star ::
          Functions.countOver("metaTotal") :: Nil
      ),
      From(column(tableName)),
      conditional,
      OrderBy(column("studyId") :: Nil),
      Limit(offset, size),
      Format("JSONEachRow"),
      Settings(Map("output_format_json_escape_forward_slashes" -> "0"))
    )
}
