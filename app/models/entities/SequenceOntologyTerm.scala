package models.entities

import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{Json, OFormat}

case class SequenceOntologyTerm(id: String, label: String)

object SequenceOntologyTerm {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val sequenceOntologyTermJsonFormatImp: OFormat[SequenceOntologyTerm] =
    Json.format[SequenceOntologyTerm]
}
