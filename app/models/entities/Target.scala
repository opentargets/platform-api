package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonNaming.SnakeCase

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
                  description: Seq[String],
//                  bioType: String,
//                  hgncId: Option[String],
//                  nameSynonyms: Seq[String],
//                  symbolSynonyms: Seq[String],
//                  genomicLocation: GenomicLocation,
//                  accessions: Seq[String]
                 )

object Target {
  object JSONImplicits {
    implicit val genomicLocationImp = Json.format[models.entities.GenomicLocation]
    implicit val targetImp = Json.format[models.entities.Target]
  }

  def apply(jObj: JsValue): Option[Target] = {
    // apply transformers for json and fill the target
    // start from internal objects and then map the external
    implicit val targetJsonConfigImp = JsonConfiguration(SnakeCase)

    import JSONImplicits._
    val source = (__ \ '_source).json.pick andThen(
      (__ \ 'id).json.pickBranch and
        (__ \ 'uniprotId).json.copyFrom( (__ \ 'uniprot_id).json.pick) and
        (__ \ 'uniprotId).json.copyFrom( (__ \ 'uniprot_id).json.pick) and
        (__ \ 'approvedSymbol).json.copyFrom( (__ \ 'approved_symbol).json.pick) and
        (__ \ 'approvedName).json.copyFrom( (__ \ 'approved_name).json.pick) and
        (__ \ 'description).json.copyFrom( (__ \ 'uniprot_function).json.pick) reduce
    )

    val transformedJObj = jObj.transform(source)

    transformedJObj.asOpt.map(obj => {
      println(Json.prettyPrint(obj))
      obj.as[Target]
    })
  }
}
