package models.entities
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult

case class Publication(
    pmid: String,
    pmcid: Option[String],
    date: String,
    year: Int,
    month: Int
)
case class Publications(count: Long,
                        earliestPubYear: Int,
                        cursor: Option[String] = None,
                        rows: IndexedSeq[Publication],
                        filteredCount: Long = 0
)

object Publications {
  implicit val getPublicationsFromDB: GetResult[Publications] =
    GetResult(r => Json.parse(r.<<[String]).as[Publications])
  def empty(withTotal: Long = 0, withLowYear: Int = 0): Publications =
    Publications(withTotal, withLowYear, None, IndexedSeq.empty)
  implicit val publicationsImpF: OFormat[Publications] = Json.format[Publications]
  implicit val publicationImpF: OFormat[Publication] = Json.format[Publication]
}
