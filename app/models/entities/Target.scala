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

case class LiteratureReference(pubmedId: String, description: String)

case class Hallmark(suppress: Boolean,
                    promote: Boolean,
                    reference: LiteratureReference,
                    label: String)

case class HallmarkAttribute(name: String, reference: LiteratureReference)

case class CancerHallmarks(hallmarks: Seq[Hallmark], attributes: Seq[HallmarkAttribute], functions: Seq[LiteratureReference])

case class Protein(id: String, accessions: Seq[String], functions: Seq[String])

case class GenomicLocation(chromosome: String, start: Long, end: Long, strand: Int)

case class SourceLink(source: String, link: String)

case class PortalProbe(note: String, chemicalprobe: String, geneSymbol: String, sourcelinks: Seq[SourceLink])

case class ChemicalProbes(probminer: String, portalprobes: Seq[PortalProbe])

case class GeneOntology(id: String, project: String, term: String, evidenceId: String)

case class TractabilityAntibodyCategories(predictedTractableMedLowConfidence: Double,
                                          clinicalPrecedence: Double,
                                          predictedTractableHighConfidence: Double)
case class TractabilitySmallMoleculeCategories(clinicalPrecedence: Double,
                                               predictedTractable: Double,
                                               discoveryPrecedence: Double)
case class TractabilityAntibody(topCategory: String, buckets: Seq[Long], categories: TractabilityAntibodyCategories)
case class TractabilitySmallMolecule(topCategory: String, smallMoleculeGenomeMember: Boolean,
                                     buckets: Seq[Long],
                                     highQualityCompounds: Long,
                                     ensemble: Double,
                                     categories: TractabilitySmallMoleculeCategories
                                    )
case class Tractability(smallmolecule: Option[TractabilitySmallMolecule], antibody: Option[TractabilityAntibody])

case class SafetyCode(code: String, mappedTerm: String, termInPaper: String)
case class SafetyReference(pubmedId: Option[String], refLabel: Option[String], refLink: Option[String])
case class AdverseEffectsActivationEffects(acuteDosing: Seq[SafetyCode], chronicDosing: Seq[SafetyCode],
                                           general: Seq[SafetyCode])
case class AdverseEffectsInhibitionEffects(acuteDosing: Seq[SafetyCode],
                                           chronicDosing: Seq[SafetyCode], general: Seq[SafetyCode],
                                           developmental: Seq[SafetyCode])

case class AdverseEffects(activationEffects: AdverseEffectsActivationEffects,
                          inhibitionEffects: AdverseEffectsInhibitionEffects,
                          organsSystemsAffected: Seq[SafetyCode],
                          references: Seq[SafetyReference])

case class SafetyRiskInfo(organsSystemsAffected: Seq[SafetyCode], references: Seq[SafetyReference],
                          safetyLiability: String)
case class Safety(adverseEffects: AdverseEffects, safetyRiskInfo: SafetyRiskInfo)

case class Target(id: String,
                  approvedSymbol: String,
                  approvedName: String,
                  bioType: String,
                  hgncId: Option[String],
                  nameSynonyms: Seq[String],
                  symbolSynonyms: Seq[String],
                  genomicLocation: GenomicLocation,
                  proteinAnnotations: Option[Protein],
                  geneOntology: Seq[GeneOntology]
                  //                  orthologs: Option[Orthologs],
                  //                  cancerHallmarks: Option[CancerHallmarks],
                  //                  chemicalProbes: Option[ChemicalProbes],
//                  safety: Option[Safety]
                 )

object Target {
  val logger = Logger(this.getClass)

  object JSONImplicits {
    // case class GeneOntology(id: String, project: String, term: String, evidenceId: String)
    implicit val geneOntologyImpW = Json.writes[models.entities.GeneOntology]
    implicit val geneOntologyImpR: Reads[models.entities.GeneOntology] =
      ((__ \ "id").read[String] and
        (__ \ "value" \ "project").read[String] and
        (__ \ "value" \ "term").read[String] and
        (__ \ "value" \ "evidence").read[String].map(_.replace(":", "_"))
        )(GeneOntology.apply _)

    implicit val proteinImpW = Json.format[models.entities.Protein]
    implicit val genomicLocationImpW = Json.format[models.entities.GenomicLocation]
    implicit val targetImpW = Json.writes[models.entities.Target]
    implicit val targetImpR: Reads[models.entities.Target] = (
      (JsPath \ "id").read[String] and
      (JsPath \ "approvedSymbol").read[String] and
      (JsPath \ "approvedName").read[String] and
      (JsPath \ "bioType").read[String] and
      (JsPath \ "hgncId").readNullable[String] and
      (JsPath \ "nameSynonyms").read[Seq[String]] and
      (JsPath \ "symbolSynonyms").read[Seq[String]] and
      (JsPath \ "genomicLocation").read[GenomicLocation] and
      (JsPath \ "proteinAnnotations").readNullable[Protein] and
      (JsPath \ "go").readNullable[Seq[GeneOntology]].map{
        case None => Seq.empty
        case Some(s) => s
      }
      )(Target.apply _)
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
