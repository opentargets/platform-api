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
        .replaceAll("""\\(nrt)""", "\\\\$1")
      val json = Json.parse(escaped)
      json.as[T]

  }
}
