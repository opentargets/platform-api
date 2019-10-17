package models.entities

import models.Entities
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Protein(id: String, accessions: Seq[String], functions: Seq[String])
case class GenomicLocation(chromosome: String, start: Long, end: Long, strand: Int)
case class Target(id: String,
                  approvedSymbol: String,
                  approvedName: String,
                  bioType: String,
                  hgncId: Option[String],
                  nameSynonyms: Seq[String],
                  symbolSynonyms: Seq[String],
                  genomicLocation: GenomicLocation,
                  proteinAnnotations: Option[Protein]
                 ) extends OTEntity

object Target {
  object JSONImplicits {
    implicit val proteinImpW = Json.format[models.entities.Protein]
    implicit val genomicLocationImpW = Json.format[models.entities.GenomicLocation]
    implicit val targetImpW = Json.format[models.entities.Target]
  }

  def fromJsValue(jObj: JsValue): Option[Target] = {
    /* apply transformers for json and fill the target
     start from internal objects and then map the external
     */
    import Target.JSONImplicits._
    val source = (__ \ '_source).json.pick
    jObj.transform(source).asOpt.map(obj => {
//      println(Json.prettyPrint(obj))
      obj.as[Target]
    })
  }
}
