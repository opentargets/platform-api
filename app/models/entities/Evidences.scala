package models.entities

import play.api.libs.json.{JsValue, Json}

case class Evidences(count: Long,
                     cursor: Option[Seq[String]],
                     rows: IndexedSeq[JsValue])

object Evidences {
  val empty = Evidences(0, None, IndexedSeq.empty)
}
