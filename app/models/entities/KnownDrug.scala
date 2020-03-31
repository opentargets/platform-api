package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

//          "drug_type" : "Small molecule",
//          "target" : "ENSG00000148229",
//          "disease" : "EFO_0000305",
//          "clinical_trial_phase" : "Phase II",
//          "mechanism_of_action" : "DNA polymerase (alpha/delta/epsilon) inhibitor",
//          "clinical_trial_status" : "Withdrawn",
//          "list_urls_counts" : 1,
//          "drug" : "CHEMBL1096882",
//          "activity" : "negative_modulator",
//          "list_urls" : [
//            {
//              "url" : "https://clinicaltrials.gov/search?id=%22NCT00006261%22",
//              "nice_name" : "Clinical Trials Information"
//            }
//          ],
//          "target_class" : [
//            "Enzyme",
//            "Transferase",
//            "Unclassified protein"
//          ]
//        }
case class URL(url: String, name: String)
case class KnownDrug(drugType: String, targetId: String, diseaseId: String,
                     drugId: String, phase: String, mechanismOfAction: String,
                     status: Option[String], activity: String, targetClass: Seq[String],
                     ctIds: Seq[String], urls: Seq[URL])

case class KnownDrugs(uniqueDrugs: Long,
                      uniqueDiseases: Long,
                      uniqueTargets: Long,
                      count: Long,
                      rows: Seq[KnownDrug])

object KnownDrug {
  val logger = Logger(this.getClass)

  // (ctPattern findAllIn str)
  val ctPattern = "NCT(\\d{8})".r

  object JSONImplicits {
    implicit val URLImpW = Json.writes[URL]
    implicit val URLImpR: Reads[URL] = (
      (__ \ "url").read[String] and
        (__ \ "nice_name").read[String]
    )(URL.apply _)

    implicit val knownDrugImpW = Json.writes[KnownDrug]
    implicit val knownDrugImpR: Reads[KnownDrug] = (
      (JsPath \ "drug_type").read[String] and
        (JsPath \ "target").read[String] and
        (JsPath \ "disease").read[String] and
        (JsPath \ "drug").read[String] and
        (JsPath \ "clinical_trial_phase").read[String] and
        (JsPath \ "mechanism_of_action").read[String] and
        (JsPath \ "clinical_trial_status").readNullable[String] and
        (JsPath \ "activity").read[String] and
        (JsPath \ "target_class").read[Seq[String]] and
        (JsPath \ "list_urls").read[Seq[Map[String, String]]].map(
          s => s.flatMap(m => ctPattern findAllIn m("url") )
        ) and
        (JsPath \ "list_urls").read[Seq[URL]]
    )(KnownDrug.apply _)

    implicit val knownDrugsImpF = Json.format[KnownDrugs]
  }
}
