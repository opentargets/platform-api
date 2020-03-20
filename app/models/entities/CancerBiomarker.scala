package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class CancerBiomarkerSource(description: Option[String], link: Option[String], name: Option[String])

case class CancerBiomarker(id: String, associationType: String, disease: String, drugName: String,
                           evidenceLevel: String, target: String, sources: Seq[CancerBiomarkerSource],
                           pubmedIds: Seq[Long])

/*
  cancerBiomarkerCount: Int!
  diseaseCount: Int!
  drugCount: Int!
 */
case class CancerBiomarkers(uniqueDrugs: Long, uniqueDiseases: Long, uniqueBiomarkers: Long,
                            rows: Seq[CancerBiomarker])

object CancerBiomarker {
  val logger = Logger(this.getClass)

  object JSONImplicits {
    implicit val cancerBiomarkerSourceImpW = Json.writes[models.entities.CancerBiomarkerSource]
    implicit val cancerBiomarkerSourceImpR: Reads[models.entities.CancerBiomarkerSource] =
      ((JsPath \ "description").readNullable[String] and
        (JsPath \ "link").readNullable[String] and
        (JsPath \ "name").readNullable[String]
      )(CancerBiomarkerSource.apply _)

    implicit val cancerBiomarkerImpW = Json.writes[models.entities.CancerBiomarker]
    implicit val cancerBiomarkerImpR: Reads[models.entities.CancerBiomarker] =
      ((JsPath \ "id").read[String] and
        (JsPath \ "associationType").read[String] and
        (JsPath \ "disease").read[String] and
        (JsPath \ "drugName").read[String] and
        (JsPath \ "evidenceLevel").read[String] and
        (JsPath \ "target").read[String] and
        (JsPath \ "sources_other").read[Seq[CancerBiomarkerSource]] and
        (JsPath \ "sources_pubmed").read[Seq[Map[String, String]]].map(_.map(m => m("pmid").toLong))
        )(CancerBiomarker.apply _)

    implicit val cancerBiomarkersImpF = Json.format[models.entities.CancerBiomarkers]
  }

  def fromJsValue(jObj: JsValue): Option[CancerBiomarker] = {
    /* apply transformers for json and fill the target
     start from internal objects and then map the external
     */
    import CancerBiomarker.JSONImplicits._

    val source = (__ \ '_source).json.pick
    jObj.transform(source).asOpt.map(obj => {
      logger.debug(Json.prettyPrint(obj))
      obj.as[CancerBiomarker]
    })
  }
}
