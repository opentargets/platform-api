package models.entities

import clickhouse.rep.SeqRep._
import clickhouse.rep.SeqRep.Implicits._
import esecuele._
import esecuele.{Functions => F}
import esecuele.Query
import models.entities.Configuration.LUTableSettings
import play.api.libs.json.Json
import slick.jdbc.GetResult

case class NetworkNode(id: String, neighbours: Vector[String])

object Network {
  def apply(network: LUTableSettings, id: String): Query = {
    val colId = Column(network.key)
    val s = Select(Seq(colId, Column(network.field.get)))
    val f = From(Column(network.name))
    val w = Where(F.equals(colId, Column.literal(id)))
    val l = Limit(0, 1)

    Query(s, f, w, l)
  }

  implicit val getNetworkNodeFromDB: GetResult[NetworkNode] = {
    GetResult(r => NetworkNode(r.<<, StrSeqRep(r.<<)))
  }

  implicit val networkNodeImp = Json.format[NetworkNode]
}
