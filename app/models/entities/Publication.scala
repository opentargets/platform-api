package models.entities

import models.Backend
import play.api.libs.json._
import sangria.schema.{Field, LongType, ListType, ObjectType, OptionType, StringType, fields}
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global

case class Publication(
    pmid: String,
    pmcid: Option[String],
    date: String,
    year: Int,
    month: Int
)

object Publication {
  implicit val getSimilarityRowFromDB: GetResult[Publication] =
    GetResult(r => Publication(r.<<, r.<<?, r.<<, r.<<, r.<<))

  implicit val similarityImp: OFormat[Publication] = Json.format[Publication]

  val publicationImp: ObjectType[Backend, JsValue] = ObjectType(
    "Publication",
    fields[Backend, JsValue](
      Field("pmid", StringType, description = None, resolve = js => (js.value \ "pmid").as[String]),
      Field(
        "pmcid",
        OptionType(StringType),
        description = None,
        resolve = js => (js.value \ "pmcid").asOpt[String]
      ),
      Field(
        "publicationDate",
        OptionType(StringType),
        description = Some("Publication Date"),
        resolve = js => (js.value \ "date").asOpt[String]
      )
    )
  )
}
