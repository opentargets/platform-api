package models.entities

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult

case class Similarity(category: String, id: String, score: Double)

object Similarity {

  implicit val getSimilarityRowFromDB: GetResult[Similarity] =
    GetResult(r => Similarity(r.<<, r.<<, r.<<))

  implicit val similarityImp: OFormat[Similarity] = Json.format[Similarity]
}
