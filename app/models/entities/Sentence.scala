package models.entities

import play.api.libs.json.{Json, OFormat}

import slick.jdbc.GetResult

object Sentence {
  implicit val getSentenceRowFromDB: GetResult[Sentence] =
    GetResult(r => Sentence(r.<<[Long].toString, r.<<, r.<<, r.<<?, r.<<?, r.<<, r.<<, r.<<, r.<<))

  implicit val sentenceImpJson: OFormat[Sentence] = Json.format[Sentence]
  implicit val sentenceBySectionImpJson: OFormat[SentenceBySection] = Json.format[SentenceBySection]

}

case class Sentence(
    pmid: String,
    section: String,
    matchedLabel: String, // label in DB
    sectionEnd: Option[Long],
    sectionStart: Option[Long],
    startInSentence: Long,
    endInSentence: Long,
    matchedType: String, // keywordType in DB
    mappedId: String // keywordId in DB
)

case class SentenceBySection(
    section: String,
    matches: List[Sentence]
)
