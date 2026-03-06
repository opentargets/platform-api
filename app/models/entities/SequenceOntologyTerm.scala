package models.entities

import play.api.libs.json.{Json, OFormat}
import utils.OTLogging
import slick.jdbc.GetResult
import utils.db.DbJsonParser.fromPositionedResult

case class SequenceOntologyTerm(id: String, label: String)

object SequenceOntologyTerm extends OTLogging {
  implicit val getSequenceOntologyTermResult: GetResult[SequenceOntologyTerm] =
    GetResult(fromPositionedResult[SequenceOntologyTerm])
  implicit val sequenceOntologyTermJsonFormatImp: OFormat[SequenceOntologyTerm] =
    Json.format[SequenceOntologyTerm]
}
