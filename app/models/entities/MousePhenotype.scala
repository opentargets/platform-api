package models.entities

import models.Helpers.logger
import play.api.{Logger, Logging}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonNaming.SnakeCase

case class GenotypePhenotype(subjectBackground: String, identifier: String,
                             label: String, pubmedId: String, subjectAllelicComposition: String)

case class MousePhenotype(categoryIdentifier: String, categoryLabel: String,
                          genotypePhenotype: Seq[GenotypePhenotype])

case class MouseGene(id: String, symbol: String, phenotypes: Seq[MousePhenotype])

case class MousePhenotypes(id: String, rows: Seq[MouseGene])

object MousePhenotype extends Logging {

  implicit val genotypePhenotypeW = Json.writes[GenotypePhenotype]
  implicit val mousePhenotypeW = Json.writes[MousePhenotype]
  implicit val mouseGeneW = Json.writes[MouseGene]
  implicit val mousePhenotypesW = Json.writes[MousePhenotypes]

  implicit val genotypePhenotypeR: Reads[GenotypePhenotype] = (
    (__ \ "subject_background").read[String] and
      (__ \ "mp_identifier").read[String] and
      (__ \ "mp_label").read[String] and
      (__ \ "pmid").read[String] and
      (__ \ "subject_allelic_composition").read[String]
    ) (GenotypePhenotype.apply _)

  implicit val mousePhenotypeR: Reads[MousePhenotype] = (
    (__ \ "category_mp_identifier").read[String] and
      (__ \ "category_mp_label").read[String] and
      (__ \ "genotype_phenotype").readWithDefault[Seq[GenotypePhenotype]](Seq.empty)
    ) (MousePhenotype.apply _)

  implicit val mouseGeneR: Reads[MouseGene] = (
    (__ \ "mouse_gene_id").read[String] and
      (__ \ "mouse_gene_symbol").read[String] and
      (__ \ "phenotypes").readWithDefault[Seq[MousePhenotype]](Seq.empty)
    ) (MouseGene.apply _)

  implicit val mousePhenotypesR: Reads[MousePhenotypes] =
    (
      (__ \ "id").read[String] and
        (__ \ "phenotypes").readWithDefault[Seq[MouseGene]](Seq.empty)
      ) (MousePhenotypes.apply _)
}


