package inputs

import play.api.libs.json.{JsValue, Json}

trait DrugWarningsInputs {
  lazy val chemblIdsWithDrugWarning: Seq[String] = Seq("CHEMBL121", "CHEMBL850", "CHEMBL468", "CHEMBL1544")
  lazy val chemblIdsWithoutDrugWarning: Seq[String] = Seq("CHEMBL350239")

  def dwQuery(id: String): JsValue = Json.parse(
    s"""{ "query": "query { drug(chemblId: \\"$id\\") { id drugWarnings { toxicityClass country description references { id source url } warningType year meddraSocCode } } }"}"""
  )
}

