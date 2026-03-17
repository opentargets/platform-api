package models.entities

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult
import utils.db.DbJsonParser.fromPositionedResult

/** @param id
  *   of gene ontology, eg: GO:0005515
  * @param label
  *   of gene ontology id, eg: protein binding
  */
case class GeneOntologyTerm(id: String, label: String)

object GeneOntologyTerm {
  implicit val getGoTermResult: GetResult[GeneOntologyTerm] =
    GetResult(fromPositionedResult[GeneOntologyTerm])
  implicit val geneOntologyF: OFormat[GeneOntologyTerm] = Json.format[GeneOntologyTerm]

}
