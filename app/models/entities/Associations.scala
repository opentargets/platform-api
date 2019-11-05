package models.entities

import clickhouse.rep.SeqRep._
import clickhouse.rep.SeqRep.Implicits._
import models.entities.Configuration.{DatasourceSettings, LUTableSettings}
import models.entities.Configuration.JSONImplicits._
import models.entities.Network.JSONImplicits._
import play.api.libs.json.Json
import slick.jdbc.GetResult

case class Association(id: String, score: Double, scorePerDS: Vector[Double])

case class Associations(network: Option[LUTableSettings],
                        node: Option[NetworkNode],
                        datasources: Seq[DatasourceSettings],
                        rows: Vector[Association])

object Associations {
  object DBImplicits {
    implicit val getAssociationRowFromDB: GetResult[Association] = {
      GetResult(r => Association(r.<<, r.<<, DSeqRep(r.<<)))
    }
  }

  object JSONImplicits {
    import models.entities.Network.JSONImplicits._
    import models.entities.Configuration.JSONImplicits._
    implicit val AssociationImp = Json.format[Association]
    implicit val associationsImp = Json.format[Associations]
  }

}
