package models.entities

import play.api.libs.json._

//type Target {

//  description: String
//  summaries: TargetSummaries!
//  details: TargetDetails!
//}

//uniprotAccessions
//safety
//proteinClassification
//
//pdb?
//pdbId?

case class GenomicLocation(chromosome: String, start: Long, end: Long, strand: Int)
case class Target(id: String,
                  uniprotId: Option[String],
                  approvedSymbol: String,
                  approvedName: String,
                  description: Option[String],
                  bioType: String,
                  hgncId: Option[String],
                  nameSynonyms: Seq[String],
                  symbolSynonyms: Seq[String],
                  genomicLocation: GenomicLocation,
                  accessions: Seq[String]
                 )

object Target {
  object JSONImplicits {
    implicit val genomicLocationImp = Json.format[models.entities.GenomicLocation]
    implicit val targetImp = Json.format[models.entities.Target]
  }

  def apply(jObj: JsValue): Option[Target] = {
    // apply transformers for json and fill the target
    // start from internal objects and then map the external
    import JSONImplicits._
    val source = (__ \ '_source).json.pick
    jObj.transform(source)
      .asOpt.map(_.as[Target])
  }
}
