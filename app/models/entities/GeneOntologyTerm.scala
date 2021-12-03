package models.entities

import play.api.libs.json.{Json, OFormat}

/** @param id  of gene ontology, eg: GO:0005515
 * @param name of gene ontology id, eg: protein binding
 */
case class GeneOntologyTerm(id: String, name: String)

object GeneOntologyTerm {

  implicit val geneOntologyF: OFormat[GeneOntologyTerm] = Json.format[GeneOntologyTerm]

}
