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
case class ClinicalTrialDrug(drugType: String, targetId: String, diseaseId: String,
                             drugId: String, phase: String, mechanismOfAction: String,
                             status: String, activity: String, targetClass: Seq[String],
                             ctIds: Seq[String])

case class ClinicalTrialDrugs(uniqueDrugs: Long,
                              uniqueDiseases: Long,
                              uniqueTargets: Long,
                              uniqueClinicalTrials: Long,
                              count: Long,
                              rows: Seq[ClinicalTrialDrug])

object ClinicalTrialDrug {
  val logger = Logger(this.getClass)

  // (ctPattern findAllIn str)
  val ctPattern = "NCT(\\d{8})".r

  object JSONImplicits {
    implicit val clinicalTrialDrugImpW = Json.writes[ClinicalTrialDrug]
    implicit val clinicalTrialDrugImpR: Reads[ClinicalTrialDrug] = (
      (JsPath \ "drug_type").read[String] and
        (JsPath \ "target").read[String] and
        (JsPath \ "disease").read[String] and
        (JsPath \ "drug").read[String] and
        (JsPath \ "clinical_trial_phase").read[String] and
        (JsPath \ "mechanism_of_action").read[String] and
        (JsPath \ "clinical_trial_status").read[String] and
        (JsPath \ "activity").read[String] and
        (JsPath \ "target_class").read[Seq[String]] and
        (JsPath \ "list_urls").read[Seq[Map[String, String]]].map(
          s => s.flatMap(m => ctPattern findAllIn m("url") )
        )
    )(ClinicalTrialDrug.apply _)

    implicit val clinicalTrialDrugsImpF = Json.format[ClinicalTrialDrugs]
  }
}
