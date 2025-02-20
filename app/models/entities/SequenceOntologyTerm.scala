package models.entities

import play.api.Logging
import play.api.libs.json.{Json, OFormat}

case class SequenceOntologyTerm(id: String, label: String)

object SequenceOntologyTerm extends Logging {
  implicit val sequenceOntologyTermJsonFormatImp: OFormat[SequenceOntologyTerm] =
    Json.format[SequenceOntologyTerm]
}
