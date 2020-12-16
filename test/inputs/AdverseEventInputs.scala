package inputs

import play.api.libs.json.{JsValue, Json}

trait AdverseEventInputs {
  lazy val aeChemblIds: Seq[String] = Seq(
    "CHEMBL2010601"
  )

  def simpleAeQuery(id: String): JsValue = Json.parse(
    s"""{ "query": "query { drug(chemblId: \\"$id\\") { id name adverseEvents { count criticalValue rows { name count logLR  } } } }"}"""
  )

}
