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

  val matchImp: ObjectType[Backend, Sentence] = ObjectType(
    "Match",
    fields[Backend, Sentence](
      Field(
        "mappedId",
        StringType,
        description = None,
        resolve = _.value.mappedId
      ),
      Field(
        "matchedLabel",
        StringType,
        description = None,
        resolve = _.value.matchedLabel
      ),
      Field(
        "sectionStart",
        OptionType(LongType),
        description = None,
        resolve = _.value.sectionStart
      ),
      Field(
        "sectionEnd",
        OptionType(LongType),
        description = None,
        resolve = _.value.sectionEnd
      ),
      Field(
        "startInSentence",
        LongType,
        description = None,
        resolve = _.value.startInSentence
      ),
      Field(
        "endInSentence",
        LongType,
        description = None,
        resolve = _.value.endInSentence
      ),
      Field(
        "matchedType",
        StringType,
        description = Some("Type of the matched label"),
        resolve = _.value.matchedType
      )
    )
  )

  val sentenceImp: ObjectType[Backend, SentenceBySection] = ObjectType(
    "Sentence",
    fields[Backend, SentenceBySection](
      Field(
        "section",
        StringType,
        description = Some("Section of the publication (either title or abstract)"),
        resolve = _.value.section
      ),
      Field(
        "matches",
        ListType(matchImp),
        description = Some("List of matches"),
        resolve = _.value.matches
      )
    )
  )

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
      ),
      Field(
        "sentences",
        OptionType(ListType(sentenceImp)),
        description = Some("Unique counts per matched keyword"),
        resolve = ctx => {
          val pmid = (ctx.value \ "pmid").as[String]
          val sentenceMap = ctx.ctx.getLiteratureSentences(pmid)
          sentenceMap.map(mp =>
            mp.keySet.foldLeft(List.empty[SentenceBySection])((acc, nxt) =>
              SentenceBySection(nxt, mp(nxt).toList) :: acc
            )
          )
        }
      )
    )
  )
}
