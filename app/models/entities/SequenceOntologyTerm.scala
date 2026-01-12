package models.entities

import play.api.libs.json.{Json, OFormat}
import utils.OTLogging

case class SequenceOntologyTerm(id: String, label: String)

object SequenceOntologyTerm extends OTLogging {

  implicit val sequenceOntologyTermJsonFormatImp: OFormat[SequenceOntologyTerm] =
    Json.format[SequenceOntologyTerm]
}
