package models.entities

import models.Helpers.logger
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonNaming.SnakeCase

case class GenotypePhenotype(subjectBackground: String, identifier: String,
                             label: String, pubmedId: String, subjectAllelicComposition: String)
case class MousePhenotype(categoryIdentifier: String, categoryLabel: String,
                          genotypePhenotype: Seq[GenotypePhenotype])
case class MousePhenotypes(id: String, rows: Seq[MousePhenotype])

object MousePhenotype {
  val logger = Logger(this.getClass)

  object JSONImplicits {
    implicit val genotypePhenotypeW = Json.writes[GenotypePhenotype]
    implicit val mousePhenotypeW = Json.writes[MousePhenotype]
    implicit val mousePhenotypesW = Json.writes[MousePhenotypes]

    implicit val tissueR: Reads[Tissue] = (
      (__ \ "subject_background").read[String] and
        (__ \ "mp_identifier").read[String] and
        (__ \ "mp_label").read[String] and
        (__ \ "pubmedId").read[String] and
        (__ \ "pubmedId").read[String]
    )(GenotypePhenotype.apply _)

    implicit val mousePhenotypeR: Reads[MousePhenotype] = (
        (__ \ "category_mp_identifier").read[String] and
        (__ \ "category_mp_label").read[String] and
        (__ \ "genotype_phenotype").readWithDefault[Seq[GenotypePhenotype]](Seq.empty)
      )(MousePhenotype.apply _)

    implicit val mousePhenotypesR: Reads[MousePhenotypes] =
      (
        (__ \ "id").read[String] and
        (__ \ "phenotypes").readWithDefault[Seq[MousePhenotype]](Seq.empty)
        )(MousePhenotypes.apply _)
  }
}


