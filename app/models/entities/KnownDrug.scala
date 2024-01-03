package models.entities

import play.api.Logging
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.util.matching.Regex

case class URL(url: String, name: String)
case class KnownDrugReference(source: String, ids: Seq[String], urls: Seq[String])

case class KnownDrug(
    approvedSymbol: String,
    approvedName: String,
    label: String,
    prefName: String,
    drugType: String,
    targetId: String,
    diseaseId: String,
    drugId: String,
    phase: Double,
    mechanismOfAction: String,
    status: Option[String],
    targetClass: Seq[String],
    references: Seq[KnownDrugReference],
    ctIds: Seq[String],
    urls: Seq[URL]
)

case class KnownDrugs(
    uniqueDrugs: Long,
    uniqueDiseases: Long,
    uniqueTargets: Long,
    count: Long,
    cursor: Option[String],
    rows: Seq[KnownDrug]
)

object KnownDrug extends Logging {
  val ctPattern: Regex = "NCT(\\d{8})".r

  implicit val KnownDrugReferenceImpJSONF: OFormat[KnownDrugReference] =
    Json.format[KnownDrugReference]
  implicit val URLImpW: OWrites[URL] = Json.writes[URL]
  implicit val URLImpR: Reads[URL] = (
    (__ \ "url").read[String] and
      (__ \ "niceName").read[String]
  )(URL.apply _)

  // approvedSymbol: String, label: String, prefName: String,
  implicit val knownDrugImpW: OWrites[KnownDrug] = Json.writes[KnownDrug]
  implicit val knownDrugImpR: Reads[KnownDrug] = (
    (__ \ "approvedSymbol").read[String] and
      (__ \ "approvedName").read[String] and
      (__ \ "label").read[String] and
      (__ \ "prefName").read[String] and
      (__ \ "drugType").read[String] and
      (__ \ "targetId").read[String] and
      (__ \ "diseaseId").read[String] and
      (__ \ "drugId").read[String] and
      (__ \ "phase").read[Double] and
      (__ \ "mechanismOfAction").read[String] and
      (__ \ "status").readNullable[String] and
      (__ \ "targetClass").readWithDefault[Seq[String]](Seq.empty) and
      (__ \ "references").readWithDefault[Seq[KnownDrugReference]](Seq.empty) and
      (__ \ "urls")
        .readWithDefault[Seq[Map[String, String]]](Seq.empty)
        .map(s => s.flatMap(m => ctPattern findAllIn m("url"))) and
      (__ \ "urls").read[Seq[URL]]
  )(KnownDrug.apply _)

  implicit val knownDrugsImpF: OFormat[KnownDrugs] = Json.format[KnownDrugs]
}
