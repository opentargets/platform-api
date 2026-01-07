package models.entities

import play.api.libs.json.{Json, OFormat}
import utils.OTLogging
import slick.jdbc.GetResult

case class SequenceOntologyTerm(id: String, label: String)

object SequenceOntologyTerm extends OTLogging {
  implicit val getSequenceOntologyTermResult: GetResult[SequenceOntologyTerm] =
    GetResult(r => Json.parse(r.<<[String]).as[SequenceOntologyTerm])
  implicit val sequenceOntologyTermJsonFormatImp: OFormat[SequenceOntologyTerm] =
    Json.format[SequenceOntologyTerm]
}
