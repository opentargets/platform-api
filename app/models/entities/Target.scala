package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Ortholog(speciesId: String,
                    name: String,
                    symbol: String,
                    support: Seq[String],
                    ensemblId: String,
                    dbId: String,
                    entrezId: String,
                    chromosomeId: String,
                    assertIds: Seq[String])

case class Orthologs(chimpanzee: Option[Seq[Ortholog]],
                     dog: Option[Seq[Ortholog]],
                     fly: Option[Seq[Ortholog]],
                     frog: Option[Seq[Ortholog]],
                     macaque: Option[Seq[Ortholog]],
                     mouse: Option[Seq[Ortholog]],
                     pig: Option[Seq[Ortholog]],
                     rat: Option[Seq[Ortholog]],
                     worm: Option[Seq[Ortholog]],
                     yeast: Option[Seq[Ortholog]],
                     zebrafish: Option[Seq[Ortholog]]
                    )

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
                  proteinAnnotations: Option[Protein],
                  orthologs: Option[Orthologs]
                 )

object Target {
  val logger = Logger(this.getClass)

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
      logger.debug(Json.prettyPrint(obj))
      obj.as[Target]
    })
  }
}
