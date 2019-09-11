package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

//safety
//proteinClassification
//
//pdb?
//pdbId?

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
                 )

object Target {
  object JSONImplicits {
    implicit val proteinImpW = Json.writes[models.entities.Protein]
    implicit val genomicLocationImpW = Json.writes[models.entities.GenomicLocation]
    implicit val targetImpW = Json.writes[models.entities.Target]

    implicit val proteinImpR: Reads[models.entities.Protein] = (
      (JsPath \ 'uniprot_id).read[String] and
        (JsPath \ 'uniprot_accessions).read[Seq[String]] and
        (JsPath \ 'uniprot_function).read[Seq[String]]
    )(models.entities.Protein.apply _)

    implicit val genomicLocationImpR: Reads[models.entities.GenomicLocation] = (
      (JsPath \ 'chromosome).read[String] and
        (JsPath \ 'gene_start).read[Long] and
        (JsPath \ 'gene_end).read[Long] and
        (JsPath \ 'strand).read[Int]
      )(models.entities.GenomicLocation.apply _)

    implicit val targetImpR: Reads[models.entities.Target] = (
      (JsPath \ 'id).read[String] and
        (JsPath \ 'approved_symbol).read[String] and
        (JsPath \ 'approved_name).read[String] and
        (JsPath \ 'biotype).read[String] and
        (JsPath \ 'hgnc_id).readNullable[String].map(p => if (p.isDefined && p.get.isEmpty) None else p) and
        (JsPath \ 'name_synonyms).read[Seq[String]] and
        (JsPath \ 'symbol_synonyms).read[Seq[String]] and
        JsPath.read[models.entities.GenomicLocation] and
      JsPath.readNullable[models.entities.Protein].map(p => if (p.isDefined && p.get.id.isEmpty) None else p)
      )(models.entities.Target.apply _)
  }

  def fromJsValue(jObj: JsValue): Option[Target] = {
    /* apply transformers for json and fill the target
     start from internal objects and then map the external
     */
    import JSONImplicits._
    val source = (__ \ '_source).json.pick
    jObj.transform(source).asOpt.map(obj => {
      println(Json.prettyPrint(obj))
      obj.as[Target]
    })
  }
}
