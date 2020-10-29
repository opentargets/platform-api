package models.entities

import play.api.libs.json.{JsValue, Json}

case class Evidences(count: Long,
                     cursor: Option[Seq[String]],
                     rows: IndexedSeq[JsValue])

object Evidences {
  def empty(withTotal: Long = 0) = Evidences(withTotal, None, IndexedSeq.empty)
}
