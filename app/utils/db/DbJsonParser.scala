package utils.db

import play.api.libs.json.{Json, OFormat, Reads}
import slick.jdbc.{GetResult, PositionedResult}

object DbJsonParser {
  def fromPositionedResult[T](positionedResult: PositionedResult)(implicit reads: Reads[T]): T = {

    val raw = positionedResult.<<[String]
    val escaped = raw
      .replaceAll("""\\([*^<>&\[\]_~])""", "$1")
      .replace("\\", "\\\\")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")
    val json = Json.parse(escaped)
    json.as[T]

  }
}
