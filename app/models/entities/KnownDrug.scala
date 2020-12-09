package models.entities

import play.api.{Logger, Logging}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class URL(url: String, name: String)

case class KnownDrug(approvedSymbol: String, approvedName: String, label: String, prefName: String,
                     drugType: String, targetId: String, diseaseId: String,
                     drugId: String, phase: String, mechanismOfAction: String,
                     status: Option[String], targetClass: Seq[String],
                     ctIds: Seq[String], urls: Seq[URL])

case class KnownDrugs(uniqueDrugs: Long,
                      uniqueDiseases: Long,
                      uniqueTargets: Long,
                      count: Long,
                      cursor: Seq[String],
                      rows: Seq[KnownDrug])

object KnownDrug extends Logging {
  val ctPattern = "NCT(\\d{8})".r

  implicit val URLImpW = Json.writes[URL]
  implicit val URLImpR: Reads[URL] = (
    (__ \ "url").read[String] and
      (__ \ "niceName").read[String]
    ) (URL.apply _)

  // approvedSymbol: String, label: String, prefName: String,
  implicit val knownDrugImpW = Json.writes[KnownDrug]
  implicit val knownDrugImpR: Reads[KnownDrug] = (
    (__ \ "approvedSymbol").read[String] and
      (__ \ "approvedName").read[String] and
      (__ \ "label").read[String] and
      (__ \ "prefName").read[String] and
      (__ \ "drugType").read[String] and
      (__ \ "targetId").read[String] and
      (__ \ "diseaseId").read[String] and
      (__ \ "drugId").read[String] and
      (__ \ "phase").read[String] and
      (__ \ "mechanismOfAction").read[String] and
      (__ \ "status").readNullable[String] and
      (__ \ "targetClass").readWithDefault[Seq[String]](Seq.empty) and
      (__ \ "references").readWithDefault[Seq[Map[String, String]]](Seq.empty).map(
        s => s.flatMap(m => ctPattern findAllIn m("url"))
      ) and
      (__ \ "urls").read[Seq[URL]]
    ) (KnownDrug.apply _)

  implicit val knownDrugsImpF = Json.format[KnownDrugs]
}
