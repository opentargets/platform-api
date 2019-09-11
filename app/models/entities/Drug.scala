package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

//type Drug {
//  id: String!
//  name: String!
//  synonyms: [String!]!
//  tradeNames: [String!]!
//  yearOfFirstApproval: String
//  type: String!
//  maximumClinicalTrialPhase: Int
//  hasBeenWithdrawn: Boolean!
//  withdrawnNotice: WithdrawnNotice
//  summaries: DrugSummaries!
//  details: DrugDetails!
//  internalCompound: Boolean!
//}

//enum DrugActivity {
//  AGONIST
//  ANTAGONIST
//  UP_OR_DOWN
//}

case class WithdrawnNotice(classes: Option[Seq[String]],
                            countries: Option[Seq[String]],
                            reasons: Option[Seq[String]],
                            year: Option[Int])

case class Drug(id: String,
                name: String,
                synonyms: Seq[String],
                tradeNames: Seq[String],
                yearOfFirstApproval: Option[Int],
                drugType: String,
                maximumClinicalTrialPhase: Option[Int],
                hasBeenWithdrawn: Boolean,
                withdrawnNotice: Option[WithdrawnNotice],
                internalCompound: Boolean
                 )

object Drug {
  object JSONImplicits {
    implicit val withdrawnNoticeImpW = Json.writes[models.entities.WithdrawnNotice]
    implicit val drugImpW = Json.writes[models.entities.Drug]

    implicit val withdrawnNoticeImpR: Reads[models.entities.WithdrawnNotice] = (
      (JsPath \ 'withdrawn_class).readNullable[Seq[String]] and
        (JsPath \ 'withdrawn_country).readNullable[Seq[String]] and
        (JsPath \ 'withdrawn_reason).readNullable[Seq[String]] and
        (JsPath \ 'withdrawn_year).readNullable[Int]
    )(models.entities.WithdrawnNotice.apply _)

    implicit val drugImpR: Reads[models.entities.Drug] = (
      (JsPath \ 'id).read[String] and
        (JsPath \ 'pref_name).read[String] and
        (JsPath \ 'synonyms).read[Seq[String]] and
        (JsPath \ 'trade_names).read[Seq[String]] and
        (JsPath \ 'year_first_approved).readNullable[Int] and
        (JsPath \ 'type).read[String] and
        (JsPath \ 'max_clinical_trial_phase).readNullable[Int] and
        (JsPath \ 'withdrawn_flag).read[Boolean] and
        (JsPath).readNullable[WithdrawnNotice].map(n => if (n.isDefined && n.get.year.isEmpty) None else n) and
        (JsPath \ 'internal_compound).read[Boolean]
      )(models.entities.Drug.apply _)
  }

  def fromJsValue(jObj: JsValue): Option[Drug] = {
    /* apply transformers for json and fill the target
     start from internal objects and then map the external
     */
    import Drug.JSONImplicits._
    val source = (__ \ '_source).json.pick
    jObj.transform(source).asOpt.map(obj => {
//      println(Json.prettyPrint(obj))
      obj.as[models.entities.Drug]
    })
  }
}
