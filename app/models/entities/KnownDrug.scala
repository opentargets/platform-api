package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class URL(url: String, name: String)
case class KnownDrug(approvedSymbol: String, approvedName: String, label: String, prefName: String,
                     drugType: String, targetId: String, diseaseId: String,
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
  val ctPattern = "NCT(\\d{8})".r

  object JSONImplicits {
    implicit val URLImpW = Json.writes[URL]
    implicit val URLImpR: Reads[URL] = (
      (__ \ "url").read[String] and
        (__ \ "nice_name").read[String]
    )(URL.apply _)

    // approvedSymbol: String, label: String, prefName: String,
    implicit val knownDrugImpW = Json.writes[KnownDrug]
    implicit val knownDrugImpR: Reads[KnownDrug] = (
      (__ \ "approvedSymbol").read[String] and
      (__ \ "approvedName").read[String] and
      (__ \ "label").read[String] and
      (__ \ "prefName").read[String] and
      (__ \ "drug_type").read[String] and
      (__ \ "target").read[String] and
      (__ \ "disease").read[String] and
      (__ \ "drug").read[String] and
      (__ \ "clinical_trial_phase").read[String] and
      (__ \ "mechanism_of_action").read[String] and
      (__ \ "clinical_trial_status").readNullable[String] and
      (__ \ "activity").read[String] and
      (__ \ "target_class").read[Seq[String]] and
      (__ \ "list_urls").read[Seq[Map[String, String]]].map(
        s => s.flatMap(m => ctPattern findAllIn m("url") )
      ) and
      (__ \ "list_urls").read[Seq[URL]]
    )(KnownDrug.apply _)

    implicit val knownDrugsImpF = Json.format[KnownDrugs]
  }
}
